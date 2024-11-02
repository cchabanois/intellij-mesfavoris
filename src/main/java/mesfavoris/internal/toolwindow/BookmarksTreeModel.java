package mesfavoris.internal.toolwindow;

import com.intellij.ui.tree.BaseTreeModel;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;

import java.util.Collections;
import java.util.List;

public class BookmarksTreeModel extends BaseTreeModel<Object> {
    private final BookmarkDatabase bookmarkDatabase;

    public BookmarksTreeModel(BookmarkDatabase bookmarkDatabase) {
        this.bookmarkDatabase = bookmarkDatabase;
    }

    @Override
    public Object getRoot() {
        return bookmarkDatabase.getBookmarksTree().getRootFolder();
    }

    @Override
    public List<?> getChildren(Object parent) {
        if (parent instanceof BookmarkFolder) {
            BookmarkFolder bookmarkFolder = (BookmarkFolder) parent;
            return bookmarkDatabase.getBookmarksTree().getChildren(bookmarkFolder.getId());
        }
        return Collections.emptyList();

    }
}
