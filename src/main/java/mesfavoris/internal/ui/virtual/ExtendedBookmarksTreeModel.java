package mesfavoris.internal.ui.virtual;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.tree.BaseTreeModel;
import mesfavoris.commons.Adapters;
import mesfavoris.internal.toolwindow.BookmarkTreeNode;
import mesfavoris.internal.toolwindow.BookmarksTreeModel;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Add {@link VirtualBookmarkFolder} and {@link BookmarkLink} support to
 * {@link BookmarksTreeModel}
 *
 * @author cchabanois
 */
public class ExtendedBookmarksTreeModel extends BaseTreeModel<Object> implements Disposable {
    private final BookmarksTreeModel bookmarksTreeModel;
    private final List<VirtualBookmarkFolder> virtualBookmarkFolders;
    private final IVirtualBookmarkFolderListener virtualBookmarkFolderListener;
    private final TreeModelListener bookmarksTreeModelListener;
    private final Map<VirtualBookmarkFolder, Map<BookmarkId, BookmarkLinkNode>> linkNodeCache = new ConcurrentHashMap<>();

    public ExtendedBookmarksTreeModel(BookmarksTreeModel bookmarksTreeModel,
                                      List<VirtualBookmarkFolder> virtualBookmarkFolders,
                                      Disposable parentDisposable) {
        this.bookmarksTreeModel = bookmarksTreeModel;
        this.virtualBookmarkFolders = virtualBookmarkFolders;

        this.virtualBookmarkFolderListener = virtualBookmarkFolder ->
            ApplicationManager.getApplication().invokeLater(() -> {
                // Refresh only the virtual bookmark folder that changed, not the entire tree
                TreePath virtualFolderPath = getTreePathForVirtualFolder(virtualBookmarkFolder);
                if (virtualFolderPath != null) {
                    // Use treeStructureChanged for structure changes (add/remove bookmarks)
                    // The cache in getChildren() will handle updating existing nodes
                    treeStructureChanged(virtualFolderPath, new int[0], new Object[0]);
                }
            });

        this.bookmarksTreeModelListener = new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                // Propagate the event
                ExtendedBookmarksTreeModel.this.treeNodesChanged(e.getTreePath(), e.getChildIndices(), e.getChildren());

                // Update BookmarkLinkNode instances in virtual folders
                Object[] children = e.getChildren();
                if (children != null) {
                    for (Object child : children) {
                        if (child instanceof BookmarkTreeNode node) {
                            updateVirtualFolderLinksForBookmark(node.getBookmark());
                        }
                    }
                }
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                ExtendedBookmarksTreeModel.this.treeNodesInserted(e.getTreePath(), e.getChildIndices(), e.getChildren());
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
                ExtendedBookmarksTreeModel.this.treeNodesRemoved(e.getTreePath(), e.getChildIndices(), e.getChildren());
            }

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                ExtendedBookmarksTreeModel.this.treeStructureChanged(e.getTreePath(), e.getChildIndices(), e.getChildren());
            }
        };

        // Add listener to the underlying BookmarksTreeModel
        bookmarksTreeModel.addTreeModelListener(bookmarksTreeModelListener);

        // Add listeners to all virtual bookmark folders
        virtualBookmarkFolders.forEach(folder -> folder.addListener(virtualBookmarkFolderListener));

        // Register this component with the parent disposable
        Disposer.register(parentDisposable, this);
    }

    @Override
    public void dispose() {
        // Remove listener from the underlying BookmarksTreeModel
        bookmarksTreeModel.removeTreeModelListener(bookmarksTreeModelListener);

        // Remove listeners from all virtual bookmark folders
        virtualBookmarkFolders.forEach(folder -> folder.removeListener(virtualBookmarkFolderListener));

        // Clear the cache
        linkNodeCache.clear();

        super.dispose();
    }

    @Override
    public Object getRoot() {
        return bookmarksTreeModel.getRoot();
    }

    @Override
    public List<Object> getChildren(Object parent) {
        // Check for VirtualBookmarkFolder first
        if (parent instanceof VirtualBookmarkFolder virtualBookmarkFolder) {
            return getVirtualBookmarkFolderChildren(virtualBookmarkFolder);
        }

        // Then check for BookmarkFolder (which includes BookmarkTreeNode)
        BookmarkFolder bookmarkFolder = Adapters.adapt(parent, BookmarkFolder.class);
        if (bookmarkFolder != null) {
            return getBookmarkFolderChildren(parent, bookmarkFolder);
        }

        return Collections.emptyList();
    }

    private List<Object> getVirtualBookmarkFolderChildren(VirtualBookmarkFolder virtualBookmarkFolder) {
        List<BookmarkLink> links = virtualBookmarkFolder.getChildren();

        // Get or create cache for this virtual folder
        Map<BookmarkId, BookmarkLinkNode> folderCache =
            linkNodeCache.computeIfAbsent(virtualBookmarkFolder, k -> new ConcurrentHashMap<>());

        // Convert BookmarkLink to BookmarkLinkNode using cache
        List<Object> result = new ArrayList<>(links.size());
        for (BookmarkLink link : links) {
            BookmarkLinkNode node = folderCache.computeIfAbsent(
                link.getBookmark().getId(),
                id -> new BookmarkLinkNode(link)
            );
            // Update the node with the latest bookmark data
            node.updateBookmarkLink(link);
            result.add(node);
        }

        // Clean up nodes that are no longer in the list
        Set<BookmarkId> currentIds = links.stream()
            .map(link -> link.getBookmark().getId())
            .collect(Collectors.toSet());
        folderCache.keySet().removeIf(id -> !currentIds.contains(id));

        return result;
    }

    private List<Object> getBookmarkFolderChildren(Object parent, BookmarkFolder bookmarkFolder) {
        // Get regular children from the database
        List<Object> children = new ArrayList<>();
        List<BookmarkTreeNode> bookmarkChildren = bookmarksTreeModel.getChildren(parent);
        children.addAll(bookmarkChildren);

        // Add virtual bookmark folders that have this folder as parent
        children.addAll(virtualBookmarkFolders.stream()
            .filter(virtualFolder -> virtualFolder.getParentId().equals(bookmarkFolder.getId()))
            .toList());

        return children;
    }

    @Override
    public boolean isLeaf(Object object) {
        if (object instanceof VirtualBookmarkFolder) {
            return false;
        }
        if (object instanceof BookmarkLinkNode) {
            return true;
        }
        return bookmarksTreeModel.isLeaf(object);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // Delegate to bookmarksTreeModel for bookmark renaming
        bookmarksTreeModel.valueForPathChanged(path, newValue);
    }

    /**
     * Refresh the tree model.
     */
    public void refresh() {
        treeStructureChanged(new TreePath(getRoot()), null, null);
    }

    private TreePath getTreePathForVirtualFolder(VirtualBookmarkFolder virtualBookmarkFolder) {
        // Get the tree path for the parent bookmark folder
        TreePath parentPath = bookmarksTreeModel.getTreePathForBookmark(virtualBookmarkFolder.getParentId()).orElse(null);
        if (parentPath == null) {
            return null;
        }

        // Append the virtual folder to the parent path
        return parentPath.pathByAddingChild(virtualBookmarkFolder);
    }

    private void updateVirtualFolderLinksForBookmark(Bookmark updatedBookmark) {
        BookmarkId bookmarkId = updatedBookmark.getId();
        for (VirtualBookmarkFolder virtualFolder : virtualBookmarkFolders) {
            Map<BookmarkId, BookmarkLinkNode> folderCache = linkNodeCache.get(virtualFolder);
            if (folderCache != null) {
                BookmarkLinkNode node = folderCache.get(bookmarkId);
                if (node != null) {
                    // Update the node with the new bookmark
                    BookmarkLink updatedLink = new BookmarkLink(
                        virtualFolder.getBookmarkFolder().getId(),
                        updatedBookmark
                    );
                    node.updateBookmarkLink(updatedLink);

                    // Fire treeNodesChanged for this specific node
                    TreePath virtualFolderPath = getTreePathForVirtualFolder(virtualFolder);
                    if (virtualFolderPath != null) {
                        List<Object> children = getChildren(virtualFolder);
                        int index = findNodeIndex(children, node);
                        if (index >= 0) {
                            treeNodesChanged(virtualFolderPath, new int[]{index}, new Object[]{node});
                        }
                    }
                }
            }
        }
    }

    private int findNodeIndex(List<Object> children, BookmarkLinkNode targetNode) {
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) instanceof BookmarkLinkNode node &&
                node.getBookmarkId().equals(targetNode.getBookmarkId())) {
                return i;
            }
        }
        return -1;
    }

}

