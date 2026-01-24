package mesfavoris.internal.toolwindow;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.tree.BaseTreeModel;
import mesfavoris.BookmarksException;
import mesfavoris.internal.service.operations.RenameBookmarkOperation;
import mesfavoris.model.*;
import mesfavoris.model.modification.*;

import javax.swing.tree.TreePath;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BookmarksTreeModel extends BaseTreeModel<BookmarkTreeNode> {
    private final BookmarkDatabase bookmarkDatabase;
    private final Map<BookmarkId, BookmarkTreeNode> nodeCache = new ConcurrentHashMap<>();

    private final IBookmarksListener bookmarksListener = modifications -> ApplicationManager.getApplication().invokeLater(() -> {
        for (BookmarksModification modification : modifications) {
            if (modification instanceof BookmarksAddedModification bookmarksAddedModification) {
                handleBookmarksAdded(bookmarksAddedModification);
            }
            if (modification instanceof BookmarkDeletedModification bookmarkDeletedModification) {
                handleBookmarkDeleted(bookmarkDeletedModification);
            }
            if (modification instanceof BookmarkPropertiesModification bookmarksPropertiesModification) {
                handleBookmarkPropertiesModified(bookmarksPropertiesModification);
            }
            if (modification instanceof BookmarksMovedModification bookmarksMovedModification) {
                handleBookmarksMoved(bookmarksMovedModification);
            }
        }
    }, ModalityState.defaultModalityState());

    private void handleBookmarksAdded(BookmarksAddedModification modification) {
        getTreePathForBookmark(modification.getParentId())
            .ifPresent(treePath -> treeStructureChanged(treePath, new int[0], new Object[0]));
    }

    private void handleBookmarkDeleted(BookmarkDeletedModification modification) {
        // Remove deleted bookmark from cache
        nodeCache.remove(modification.getBookmarkId());
        getTreePathForBookmark(modification.getBookmarkParentId())
            .ifPresent(treePath -> treeStructureChanged(treePath, new int[0], new Object[0]));
    }

    private void handleBookmarkPropertiesModified(BookmarkPropertiesModification modification) {
        BookmarkId bookmarkId = modification.getBookmarkId();
        BookmarkId parentBookmarkId = modification.getTargetTree()
            .getParentBookmark(bookmarkId).getId();

        // Update the node with the new bookmark instance
        BookmarkTreeNode node = nodeCache.get(bookmarkId);
        if (node != null) {
            Bookmark updatedBookmark = modification.getTargetTree().getBookmark(bookmarkId);
            node.updateBookmark(updatedBookmark);

            // Now we can use treeNodesChanged since the node itself hasn't changed
            getTreePathForBookmark(parentBookmarkId).ifPresent(parentPath -> {
                List<BookmarkTreeNode> children = getChildren(parentPath.getLastPathComponent());
                int index = findNodeIndex(children, node);
                if (index >= 0) {
                    treeNodesChanged(parentPath, new int[]{index}, new Object[]{node});
                }
            });
        } else {
            // Fallback if node not in cache
            getTreePathForBookmark(parentBookmarkId)
                .ifPresent(treePath -> treeStructureChanged(treePath, new int[0], new Object[0]));
        }
    }

    private void handleBookmarksMoved(BookmarksMovedModification modification) {
        getTreePathForBookmark(modification.getNewParentId())
            .ifPresent(treePath -> treeStructureChanged(treePath, new int[0], new Object[0]));
        getTreePathForBookmark(modification.getOldParentId())
            .ifPresent(treePath -> treeStructureChanged(treePath, new int[0], new Object[0]));
    }


    public BookmarksTreeModel(BookmarkDatabase bookmarkDatabase, Disposable parentDisposable) {
        this.bookmarkDatabase = bookmarkDatabase;
        bookmarkDatabase.addListener(bookmarksListener);

        // Register this component with the parent disposable
        Disposer.register(parentDisposable, this);
    }

    @Override
    public void dispose() {
        bookmarkDatabase.removeListener(bookmarksListener);
        nodeCache.clear();
        super.dispose();
    }

    @Override
    public BookmarkTreeNode getRoot() {
        BookmarkFolder rootFolder = bookmarkDatabase.getBookmarksTree().getRootFolder();
        return getOrCreateNode(rootFolder);
    }

    /**
     * Get the BookmarkTreeNode from a TreePath
     */
    public BookmarkTreeNode getBookmarkTreeNode(TreePath path) {
        return (BookmarkTreeNode) path.getLastPathComponent();
    }

    @Override
    public List<BookmarkTreeNode> getChildren(Object parent) {
        BookmarkTreeNode parentNode = (BookmarkTreeNode) parent;
        if (parentNode.isFolder()) {
            List<Bookmark> bookmarks = bookmarkDatabase.getBookmarksTree().getChildren(parentNode.getId());
            List<BookmarkTreeNode> nodes = new ArrayList<>(bookmarks.size());
            for (Bookmark bookmark : bookmarks) {
                nodes.add(getOrCreateNode(bookmark));
            }
            return nodes;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isLeaf(Object object) {
        BookmarkTreeNode node = (BookmarkTreeNode) object;
        return !node.isFolder();
    }

    private BookmarkTreeNode getOrCreateNode(Bookmark bookmark) {
        return nodeCache.computeIfAbsent(bookmark.getId(), id -> new BookmarkTreeNode(bookmark));
    }

    private int findNodeIndex(List<BookmarkTreeNode> nodes, BookmarkTreeNode targetNode) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().equals(targetNode.getId())) {
                return i;
            }
        }
        return -1;
    }

    public Optional<TreePath> getTreePathForBookmark(BookmarkId bookmarkId) {
        Bookmark bookmark = bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId);
        if (bookmark == null) {
            return Optional.empty();
        }
        List<Object> path = new ArrayList<>();
        Bookmark current = bookmark;
        while (current != null) {
            path.add(0, getOrCreateNode(current));
            current = bookmarkDatabase.getBookmarksTree().getParentBookmark(current.getId());
        }
        return Optional.of(new TreePath(path.toArray()));
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        BookmarkTreeNode node = getBookmarkTreeNode(path);
        if (node != null && newValue instanceof String newName) {
            RenameBookmarkOperation operation = new RenameBookmarkOperation(bookmarkDatabase);
            try {
                operation.renameBookmark(node.getId(), newName);
            } catch (BookmarksException e) {
                Notification notification = new Notification(
                        "com.cchabanois.mesfavoris.errors",
                        "Cannot rename bookmark",
                        e.getMessage(),
                        NotificationType.ERROR
                );
                Notifications.Bus.notify(notification);
            }
        }
    }

    public BookmarkDatabase getBookmarkDatabase() {
        return bookmarkDatabase;
    }
}
