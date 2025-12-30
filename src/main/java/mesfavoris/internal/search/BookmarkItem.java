package mesfavoris.internal.search;

import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarksTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper for Bookmark to be used in Search Everywhere
 */
public class BookmarkItem {
    private final BookmarksTree bookmarksTree;
    private final Bookmark bookmark;

    public BookmarkItem(@Nullable BookmarksTree bookmarksTree, @NotNull Bookmark bookmark) {
        this.bookmarksTree = bookmarksTree;
        this.bookmark = bookmark;
    }

    @NotNull
    public Bookmark getBookmark() {
        return bookmark;
    }

    @Nullable
    public BookmarksTree getBookmarksTree() {
        return bookmarksTree;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookmarkItem that = (BookmarkItem) o;
        return bookmark.getId().equals(that.bookmark.getId());
    }

    @Override
    public int hashCode() {
        return bookmark.getId().hashCode();
    }
}

