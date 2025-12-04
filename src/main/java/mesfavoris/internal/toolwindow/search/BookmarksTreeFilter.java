package mesfavoris.internal.toolwindow.search;

import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarksTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Filter for bookmarks tree based on search text
 */
public class BookmarksTreeFilter {
    private final BookmarkDatabase bookmarkDatabase;
    private String searchText;
    private final Set<Bookmark> matchingBookmarks;
    private final Set<Bookmark> visibleBookmarks;

    public BookmarksTreeFilter(@NotNull BookmarkDatabase bookmarkDatabase) {
        this.bookmarkDatabase = bookmarkDatabase;
        this.searchText = "";
        this.matchingBookmarks = new HashSet<>();
        this.visibleBookmarks = new HashSet<>();
    }

    public void setSearchText(@Nullable String searchText) {
        this.searchText = searchText == null ? "" : searchText.toLowerCase();
        updateFilter();
    }

    public String getSearchText() {
        return searchText;
    }

    public boolean isFiltering() {
        return !searchText.isEmpty();
    }

    public boolean isVisible(@NotNull Bookmark bookmark) {
        if (!isFiltering()) {
            return true;
        }
        return visibleBookmarks.contains(bookmark);
    }

    public boolean matches(@NotNull Bookmark bookmark) {
        return matchingBookmarks.contains(bookmark);
    }

    private void updateFilter() {
        matchingBookmarks.clear();
        visibleBookmarks.clear();

        if (!isFiltering()) {
            return;
        }

        BookmarksTree bookmarksTree = bookmarkDatabase.getBookmarksTree();

        // Find all matching bookmarks
        findMatchingBookmarks(bookmarksTree, bookmarksTree.getRootFolder());

        // Make all ancestors of matching bookmarks visible
        for (Bookmark matching : matchingBookmarks) {
            makeAncestorsVisible(bookmarksTree, matching);
        }
    }

    private void findMatchingBookmarks(@NotNull BookmarksTree bookmarksTree, @NotNull Bookmark bookmark) {
        if (matchesSearchText(bookmark)) {
            matchingBookmarks.add(bookmark);
            visibleBookmarks.add(bookmark);
        }

        if (bookmark instanceof BookmarkFolder folder) {
            List<Bookmark> children = bookmarksTree.getChildren(folder.getId());
            for (Bookmark child : children) {
                findMatchingBookmarks(bookmarksTree, child);
            }
        }
    }

    private boolean matchesSearchText(@NotNull Bookmark bookmark) {
        String name = bookmark.getPropertyValue(Bookmark.PROPERTY_NAME);
        if (name != null && name.toLowerCase().contains(searchText)) {
            return true;
        }

        String comment = bookmark.getPropertyValue(Bookmark.PROPERTY_COMMENT);
        if (comment != null && comment.toLowerCase().contains(searchText)) {
            return true;
        }

        return false;
    }

    private void makeAncestorsVisible(@NotNull BookmarksTree bookmarksTree, @NotNull Bookmark bookmark) {
        Bookmark current = bookmark;
        while (current != null) {
            visibleBookmarks.add(current);
            Bookmark parent = bookmarksTree.getParentBookmark(current.getId());
            if (parent == null || parent.equals(bookmarksTree.getRootFolder())) {
                break;
            }
            current = parent;
        }
    }
}

