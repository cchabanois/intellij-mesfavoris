package mesfavoris.internal.toolwindow.search;

import mesfavoris.model.*;
import mesfavoris.tags.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Filter for bookmarks tree. Supports two mutually exclusive modes:
 * - text-based: filters by name/comment match
 * - ID-based: shows only a specific set of bookmark IDs (used by MCP tools)
 */
public class BookmarksTreeFilter {
    private final BookmarkDatabase bookmarkDatabase;
    private String searchText;
    private final Set<Bookmark> matchingBookmarks;
    private final Set<Bookmark> visibleBookmarks;
    private Set<BookmarkId> idFilter = null;

    public BookmarksTreeFilter(@NotNull BookmarkDatabase bookmarkDatabase) {
        this.bookmarkDatabase = bookmarkDatabase;
        this.searchText = "";
        this.matchingBookmarks = new HashSet<>();
        this.visibleBookmarks = new HashSet<>();
    }

    /**
     * Switches to text mode and filters bookmarks by name/comment match. Clears any active ID filter.
     *
     * @param searchText the text to search for, or null/empty to clear the text filter
     */
    public void setSearchText(@Nullable String searchText) {
        this.idFilter = null;
        this.searchText = searchText == null ? "" : searchText.toLowerCase();
        updateFilter();
    }

    /**
     * Returns the current search text.
     *
     * @return the search text, or an empty string when not in text mode
     */
    public String getSearchText() {
        return searchText;
    }

    /**
     * Switches to ID mode and shows only the given bookmarks (plus their ancestors). Clears search text.
     *
     * @param ids the bookmark IDs to show exclusively
     */
    public void setIdFilter(@NotNull Set<BookmarkId> ids) {
        this.idFilter = ids;
        this.searchText = "";
        updateFilter();
    }

    /**
     * Clears all active filters; the tree shows all bookmarks.
     */
    public void clear() {
        this.idFilter = null;
        this.searchText = "";
        updateFilter();
    }

    /**
     * Returns true when the filter is in ID mode.
     *
     * @return true if an ID filter is active
     */
    public boolean isIdFiltering() {
        return idFilter != null;
    }

    /**
     * Returns true when any filter (text or ID) is active.
     *
     * @return true if the tree is currently filtered
     */
    public boolean isFiltering() {
        return isIdFiltering() || !searchText.isEmpty();
    }

    /**
     * Returns true if the bookmark should be shown in the tree. Always true when no filter is active.
     *
     * @param bookmark the bookmark to check
     * @return true if the bookmark is visible
     */
    public boolean isVisible(@NotNull Bookmark bookmark) {
        if (!isFiltering()) {
            return true;
        }
        return visibleBookmarks.contains(bookmark);
    }

    /**
     * Returns true if the bookmark directly matches the active filter (used to bold matched nodes).
     *
     * @param bookmark the bookmark to check
     * @return true if the bookmark is a direct match
     */
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

        if (isIdFiltering()) {
            collectIdMatches(bookmarksTree);
        } else {
            collectTextMatches(bookmarksTree, bookmarksTree.getRootFolder());
        }

        for (Bookmark matching : matchingBookmarks) {
            makeAncestorsVisible(bookmarksTree, matching);
        }
    }

    private void collectIdMatches(@NotNull BookmarksTree bookmarksTree) {
        for (BookmarkId id : idFilter) {
            Bookmark bookmark = bookmarksTree.getBookmark(id);
            if (bookmark != null) {
                matchingBookmarks.add(bookmark);
            }
        }
    }

    private void collectTextMatches(@NotNull BookmarksTree bookmarksTree, @NotNull Bookmark bookmark) {
        if (matchesSearchText(bookmark)) {
            matchingBookmarks.add(bookmark);
            visibleBookmarks.add(bookmark);
        }

        if (bookmark instanceof BookmarkFolder folder) {
            List<Bookmark> children = bookmarksTree.getChildren(folder.getId());
            for (Bookmark child : children) {
                collectTextMatches(bookmarksTree, child);
            }
        }
    }

    private boolean matchesSearchText(@NotNull Bookmark bookmark) {
        // explicit tag query (tag:xxx or #xxx) matches only against tags
        String tagTerm = Tags.extractTagQuery(searchText);
        if (tagTerm != null) {
            return Tags.hasTagMatching(bookmark, tagTerm);
        }

        String name = bookmark.getPropertyValue(Bookmark.PROPERTY_NAME);
        if (name != null && name.toLowerCase().contains(searchText)) {
            return true;
        }

        String comment = bookmark.getPropertyValue(Bookmark.PROPERTY_COMMENT);
        if (comment != null && comment.toLowerCase().contains(searchText)) {
            return true;
        }

        // free text also matches tags
        return Tags.hasTagMatching(bookmark, searchText);
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
