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
import mesfavoris.commons.Adapters;
import mesfavoris.internal.service.operations.RenameBookmarkOperation;
import mesfavoris.model.*;
import mesfavoris.model.modification.*;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BookmarksTreeModel extends BaseTreeModel<Bookmark> {
    private final BookmarkDatabase bookmarkDatabase;

    private final IBookmarksListener bookmarksListener = modifications -> ApplicationManager.getApplication().invokeLater(() -> {
        for (BookmarksModification modification : modifications) {
            if (modification instanceof BookmarksAddedModification bookmarksAddedModification) {
                getTreePathForBookmark(bookmarksAddedModification.getParentId())
                    .ifPresent(treePath -> treeStructureChanged(treePath, new int[0], new Object[0]));
            }
            if (modification instanceof BookmarkDeletedModification bookmarkDeletedModification) {
                getTreePathForBookmark(bookmarkDeletedModification.getBookmarkParentId())
                    .ifPresent(treePath -> treeStructureChanged(treePath, new int[0], new Object[0]));
            }
            if (modification instanceof BookmarkPropertiesModification bookmarksPropertiesModification) {
                BookmarkId parentBookmarkId = bookmarksPropertiesModification.getTargetTree()
                    .getParentBookmark(bookmarksPropertiesModification.getBookmarkId()).getId();
                // we cannot use treeNodesChanged because we use immutable objects and a new Bookmark is created each time bookmark is modified
                // treeNodesChanged is for changes on a given (mutable) object
                getTreePathForBookmark(parentBookmarkId)
                    .ifPresent(treePath -> treeStructureChanged(treePath, new int[0], new Object[0]));
            }
            if (modification instanceof BookmarksMovedModification bookmarksMovedModification) {
                getTreePathForBookmark(bookmarksMovedModification.getNewParentId())
                    .ifPresent(treePath -> treeStructureChanged(treePath, new int[0], new Object[0]));
                getTreePathForBookmark(bookmarksMovedModification.getOldParentId())
                        .ifPresent(treePath -> treeStructureChanged(treePath, new int[0], new Object[0]));
            }
        }
    }, ModalityState.defaultModalityState());

    public BookmarksTreeModel(BookmarkDatabase bookmarkDatabase, Disposable parentDisposable) {
        this.bookmarkDatabase = bookmarkDatabase;
        bookmarkDatabase.addListener(bookmarksListener);

        // Register this component with the parent disposable
        Disposer.register(parentDisposable, this);
    }

    @Override
    public void dispose() {
        bookmarkDatabase.removeListener(bookmarksListener);
        super.dispose();
    }

    @Override
    public BookmarkFolder getRoot() {
        return bookmarkDatabase.getBookmarksTree().getRootFolder();
    }

    @Override
    public List<Bookmark> getChildren(Object parent) {
        if (parent instanceof BookmarkFolder bookmarkFolder) {
            return bookmarkDatabase.getBookmarksTree().getChildren(bookmarkFolder.getId());
        }
        return Collections.emptyList();

    }

    @Override
    public boolean isLeaf(Object object) {
        return !(object instanceof BookmarkFolder);
    }

    public Optional<TreePath> getTreePathForBookmark(BookmarkId bookmarkId) {
        Bookmark bookmark = bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId);
        return getTreePathForBookmark(bookmark);
    }

    public Optional<TreePath> getTreePathForBookmark(Bookmark bookmark) {
        if (bookmark == null) {
            return Optional.empty();
        }
        List<Object> path = new ArrayList<>();
        while (bookmark != null) {
            path.add(0, bookmark);
            bookmark = bookmarkDatabase.getBookmarksTree().getParentBookmark(bookmark.getId());
        }
        return Optional.of(new TreePath(path.toArray()));
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        Bookmark bookmark = getBookmark(path);
        if (bookmark != null && newValue instanceof String newName) {
            RenameBookmarkOperation operation = new RenameBookmarkOperation(bookmarkDatabase);
            try {
                operation.renameBookmark(bookmark.getId(), newName);
            } catch (BookmarksException e) {
                Notification notification = new Notification(
                        "com.cchabanois.mesfavoris.errors",
                        "Cannot rename bookmark",
                        e.getMessage(),
                        NotificationType.ERROR // ou ERROR
                );
                Notifications.Bus.notify(notification);
            }
        }
    }

    private Bookmark getBookmark(TreePath path) {
        Object object = path.getLastPathComponent();
        return Adapters.adapt(object, Bookmark.class);
    }

    public BookmarkDatabase getBookmarkDatabase() {
        return bookmarkDatabase;
    }


}
