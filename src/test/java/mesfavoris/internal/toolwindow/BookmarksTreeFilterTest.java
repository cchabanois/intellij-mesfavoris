package mesfavoris.internal.toolwindow;

import mesfavoris.internal.toolwindow.search.BookmarksTreeFilter;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import org.junit.Before;
import org.junit.Test;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;

public class BookmarksTreeFilterTest {
    private BookmarkDatabase bookmarkDatabase;
    private BookmarksTreeFilter filter;
    
    private BookmarkId bookmark1Id;
    private BookmarkId bookmark2Id;
    private BookmarkId bookmark3Id;
    private BookmarkId folder1Id;
    private BookmarkId folder2Id;

    @Before
    public void setUp() {
        // Given
        bookmark1Id = new BookmarkId("bookmark1");
        bookmark2Id = new BookmarkId("bookmark2");
        bookmark3Id = new BookmarkId("bookmark3");
        folder1Id = new BookmarkId("folder1");
        folder2Id = new BookmarkId("folder2");

        BookmarksTree bookmarksTree = bookmarksTree("root")
                .addBookmarks("root",
                        bookmark(bookmark1Id, "Java Tutorial")
                                .withProperty(Bookmark.PROPERTY_COMMENT, "Learn Java basics"),
                        bookmark(bookmark2Id, "Python Guide")
                                .withProperty(Bookmark.PROPERTY_COMMENT, "Advanced Python"),
                        bookmarkFolder(folder1Id, "Development"))
                .addBookmarks(folder1Id,
                        bookmark(bookmark3Id, "IntelliJ IDEA")
                                .withProperty(Bookmark.PROPERTY_COMMENT, "IDE for Java development"))
                .addBookmarks("root",
                        bookmarkFolder(folder2Id, "Resources"))
                .build();

        bookmarkDatabase = new BookmarkDatabase("test", bookmarksTree);
        filter = new BookmarksTreeFilter(bookmarkDatabase);
    }

    @Test
    public void testNoFilteringWhenSearchTextIsEmpty() {
        // When
        filter.setSearchText("");

        // Then
        assertThat(filter.isFiltering()).isFalse();
        assertThat(filter.isVisible(getBookmark(bookmark1Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark2Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark3Id))).isTrue();
    }

    @Test
    public void testNoFilteringWhenSearchTextIsNull() {
        // When
        filter.setSearchText(null);

        // Then
        assertThat(filter.isFiltering()).isFalse();
        assertThat(filter.isVisible(getBookmark(bookmark1Id))).isTrue();
    }

    @Test
    public void testFilterByName() {
        // When
        filter.setSearchText("Java");

        // Then
        assertThat(filter.isFiltering()).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark1Id))).isTrue();
        assertThat(filter.matches(getBookmark(bookmark1Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark2Id))).isFalse();
        assertThat(filter.matches(getBookmark(bookmark2Id))).isFalse();
    }

    @Test
    public void testFilterByComment() {
        // When
        filter.setSearchText("Advanced");

        // Then
        assertThat(filter.isFiltering()).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark2Id))).isTrue();
        assertThat(filter.matches(getBookmark(bookmark2Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark1Id))).isFalse();
        assertThat(filter.matches(getBookmark(bookmark1Id))).isFalse();
    }

    @Test
    public void testFilterIsCaseInsensitive() {
        // When
        filter.setSearchText("PYTHON");

        // Then
        assertThat(filter.isVisible(getBookmark(bookmark2Id))).isTrue();
        assertThat(filter.matches(getBookmark(bookmark2Id))).isTrue();
    }

    @Test
    public void testParentFolderIsVisibleWhenChildMatches() {
        // When
        filter.setSearchText("IntelliJ");

        // Then
        assertThat(filter.isVisible(getBookmark(bookmark3Id))).isTrue();
        assertThat(filter.matches(getBookmark(bookmark3Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(folder1Id))).isTrue();
        assertThat(filter.matches(getBookmark(folder1Id))).isFalse();
    }

    @Test
    public void testFolderNotVisibleWhenNoChildMatches() {
        // When
        filter.setSearchText("Java");

        // Then
        assertThat(filter.isVisible(getBookmark(folder2Id))).isFalse();
        assertThat(filter.matches(getBookmark(folder2Id))).isFalse();
    }

    @Test
    public void testPartialMatch() {
        // When
        filter.setSearchText("dev");

        // Then
        assertThat(filter.isVisible(getBookmark(folder1Id))).isTrue();
        assertThat(filter.matches(getBookmark(folder1Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark3Id))).isTrue();
        assertThat(filter.matches(getBookmark(bookmark3Id))).isTrue();
    }

    @Test
    public void testGetSearchText() {
        // When
        filter.setSearchText("test search");

        // Then
        assertThat(filter.getSearchText()).isEqualTo("test search");
    }

    private Bookmark getBookmark(BookmarkId bookmarkId) {
        return bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId);
    }
}

