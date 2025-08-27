package mesfavoris.internal.toolwindow;

import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import mesfavoris.commons.Adapters;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;

import javax.swing.tree.TreePath;

public class BookmarksTreeComponent extends DnDAwareTree implements Disposable {
    private final BookmarksTreeModel bookmarksTreeModel;

    public BookmarksTreeComponent(BookmarkDatabase bookmarkDatabase, Disposable parentDisposable) {
        super();
        this.bookmarksTreeModel = new BookmarksTreeModel(bookmarkDatabase, this);
        setModel(this.bookmarksTreeModel);
        setRootVisible(false);

        Disposer.register(parentDisposable, this);
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

    public TreePath getTreePathForBookmark(BookmarkId bookmarkId) {
        return bookmarksTreeModel.getTreePathForBookmark(bookmarkId);
    }

    public Bookmark getBookmark(TreePath path) {
        Object object = path.getLastPathComponent();
        return Adapters.adapt(object, Bookmark.class);
    }

    @Override
    public void dispose() {
    }
}
