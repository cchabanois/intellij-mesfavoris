package mesfavoris.internal.toolwindow;

import com.intellij.openapi.Disposable;
import mesfavoris.internal.toolwindow.search.BookmarksTreeFilter;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;

import javax.swing.tree.TreePath;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filtered tree model that wraps BookmarksTreeModel and applies filtering
 */
public class FilteredBookmarksTreeModel extends BookmarksTreeModel {
    private final BookmarksTreeFilter filter;

    public FilteredBookmarksTreeModel(BookmarkDatabase bookmarkDatabase, BookmarksTreeFilter filter, Disposable parentDisposable) {
        super(bookmarkDatabase, parentDisposable);
        this.filter = filter;
    }

    @Override
    public List<Bookmark> getChildren(Object parent) {
        List<Bookmark> children = super.getChildren(parent);
        
        if (!filter.isFiltering()) {
            return children;
        }

        // Filter children to only show visible ones
        return children.stream()
                .filter(filter::isVisible)
                .collect(Collectors.toList());
    }

    public BookmarksTreeFilter getFilter() {
        return filter;
    }

    public void refresh() {
        treeStructureChanged(new TreePath(getRoot()), null, null);
    }
}

