package mesfavoris.internal.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.tree.BaseTreeModel;
import mesfavoris.model.*;
import mesfavoris.model.modification.BookmarkDeletedModification;
import mesfavoris.model.modification.BookmarkPropertiesModification;
import mesfavoris.model.modification.BookmarksAddedModification;
import mesfavoris.model.modification.BookmarksModification;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookmarksTreeModel extends BaseTreeModel<Bookmark> {
    private final BookmarkDatabase bookmarkDatabase;

    private final IBookmarksListener bookmarksListener = modifications -> ApplicationManager.getApplication().invokeLater(() -> {
        for (BookmarksModification modification : modifications) {
            if (modification instanceof BookmarksAddedModification bookmarksAddedModification) {
                treeStructureChanged(getTreePathForBookmark(bookmarksAddedModification.getParentId()), new int[0], new Object[0]);
            }
            if (modification instanceof BookmarkDeletedModification bookmarkDeletedModification) {
                treeStructureChanged(getTreePathForBookmark(bookmarkDeletedModification.getBookmarkParentId()), new int[0], new Object[0]);
            }
            if (modification instanceof BookmarkPropertiesModification bookmarksPropertiesModification) {
                Bookmark bookmark = bookmarksPropertiesModification.getTargetTree().getBookmark(bookmarksPropertiesModification.getBookmarkId());
                BookmarkId parentBookmarkId = bookmarksPropertiesModification.getTargetTree().getParentBookmark(bookmarksPropertiesModification.getBookmarkId()).getId();
                int index = bookmarksPropertiesModification.getTargetTree().getChildren(parentBookmarkId).indexOf(bookmark);
                treeNodesChanged(getTreePathForBookmark(parentBookmarkId), new int[] { index }, new Object[] { bookmark });
            }
        }
    }, ModalityState.defaultModalityState());

    public BookmarksTreeModel(BookmarkDatabase bookmarkDatabase) {
        this.bookmarkDatabase = bookmarkDatabase;
        bookmarkDatabase.addListener(bookmarksListener);
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

    public TreePath getTreePathForBookmark(BookmarkId bookmarkId) {
        return getTreePathForBookmark(bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId));
    }

    public TreePath getTreePathForBookmark(Bookmark bookmark) {
        List<Object> path = new ArrayList<>();
        while (bookmark != null) {
            path.add(0, bookmark);
            bookmark = bookmarkDatabase.getBookmarksTree().getParentBookmark(bookmark.getId());
        }
        return new TreePath(path.toArray());
    }

}
