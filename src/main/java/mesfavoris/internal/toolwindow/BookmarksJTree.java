package mesfavoris.internal.toolwindow;

import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.openapi.Disposable;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;

public class BookmarksJTree extends DnDAwareTree implements Disposable {
    private final BookmarksTreeModel bookmarksTreeModel;

    public BookmarksJTree(BookmarkDatabase bookmarkDatabase) {
        super();
        this.bookmarksTreeModel = new BookmarksTreeModel(bookmarkDatabase);
        setModel(this.bookmarksTreeModel);
        setRootVisible(false);
    }

    @Override
    public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value instanceof Bookmark bookmark) {
            String name = bookmark.getPropertyValue(Bookmark.PROPERTY_NAME);
            if (name == null) {
                name = "unnamed";
            }
            return name;
        } else {
            return super.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
        }
    }

    @Override
    public void dispose() {
        this.bookmarksTreeModel.dispose();
    }
}
