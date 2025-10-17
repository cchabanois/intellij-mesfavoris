package mesfavoris.internal.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.treeStructure.Tree;
import mesfavoris.commons.Adapters;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Optional;

public class BookmarksTreeComponent extends Tree implements Disposable {
    private final BookmarksTreeModel bookmarksTreeModel;
    private final BookmarkDatabase bookmarkDatabase;

    public BookmarksTreeComponent(BookmarkDatabase bookmarkDatabase, Disposable parentDisposable) {
        super();
        this.bookmarkDatabase = bookmarkDatabase;
        this.bookmarksTreeModel = new BookmarksTreeModel(bookmarkDatabase, this);
        setModel(this.bookmarksTreeModel);
        setRootVisible(false);

        // Install custom cell editor that prevents inline editing via mouse events
        setCellEditor(new DefaultTreeCellEditor(this, new DefaultTreeCellRenderer()) {
            @Override
            public boolean isCellEditable(EventObject event) {
                // Only allow programmatic editing (not from mouse events)
                return !(event instanceof MouseEvent);
            }
        });

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

    @Override
    public boolean isPathEditable(TreePath path) {
        Bookmark bookmark = getBookmark(path);
        if (bookmark == null) {
            return false;
        }
        return bookmarkDatabase.getBookmarksModificationValidator()
                .validateModification(bookmarkDatabase.getBookmarksTree(), bookmark.getId())
                .isOk();
    }

    public Optional<TreePath> getTreePathForBookmark(BookmarkId bookmarkId) {
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
