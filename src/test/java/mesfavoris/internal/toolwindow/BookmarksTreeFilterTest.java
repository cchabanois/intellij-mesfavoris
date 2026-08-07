package mesfavoris.internal.toolwindow;

import mesfavoris.internal.toolwindow.search.BookmarksTreeFilter;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tags.TagsBookmarkProperties.PROP_TAGS;
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
                                .withProperty(Bookmark.PROPERTY_COMMENT, "Learn Java basics")
                                .withProperty(PROP_TAGS, "favorite,beginner"),
                        bookmark(bookmark2Id, "Python Guide")
                                .withProperty(Bookmark.PROPERTY_COMMENT, "Advanced Python")
                                .withProperty(PROP_TAGS, "reference"),
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
    public void testFilterByTagQuery() {
        // When
        filter.setSearchText("tag:favorite");

        // Then - only the bookmark tagged "favorite" matches (not by name/comment)
        assertThat(filter.isVisible(getBookmark(bookmark1Id))).isTrue();
        assertThat(filter.matches(getBookmark(bookmark1Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark2Id))).isFalse();
    }

    @Test
    public void testFilterByHashTagQuery() {
        // When
        filter.setSearchText("#reference");

        // Then
        assertThat(filter.isVisible(getBookmark(bookmark2Id))).isTrue();
        assertThat(filter.matches(getBookmark(bookmark2Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark1Id))).isFalse();
    }

    @Test
    public void testFreeTextAlsoMatchesTags() {
        // When - free text (no tag: prefix) still matches a tag
        filter.setSearchText("beginner");

        // Then
        assertThat(filter.isVisible(getBookmark(bookmark1Id))).isTrue();
        assertThat(filter.matches(getBookmark(bookmark1Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark2Id))).isFalse();
    }

    @Test
    public void testGetSearchText() {
        // When
        filter.setSearchText("test search");

        // Then
        assertThat(filter.getSearchText()).isEqualTo("test search");
    }

    @Test
    public void testSetIdFilter_showsOnlySpecifiedBookmarksAndAncestors() {
        // When - filter by bookmark3Id (which is inside folder1)
        filter.setIdFilter(Set.of(bookmark3Id));

        // Then - bookmark3 and its parent folder1 are visible
        assertThat(filter.isFiltering()).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark3Id))).isTrue();
        assertThat(filter.matches(getBookmark(bookmark3Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(folder1Id))).isTrue();
        assertThat(filter.matches(getBookmark(folder1Id))).isFalse();

        // Other bookmarks not in the filter are not visible
        assertThat(filter.isVisible(getBookmark(bookmark1Id))).isFalse();
        assertThat(filter.isVisible(getBookmark(bookmark2Id))).isFalse();
        assertThat(filter.isVisible(getBookmark(folder2Id))).isFalse();
    }

    @Test
    public void testClearIdFilter_restoresAllBookmarks() {
        // Given - an ID filter is active
        filter.setIdFilter(Set.of(bookmark3Id));
        assertThat(filter.isFiltering()).isTrue();

        // When
        filter.clear();

        // Then - all bookmarks are visible again
        assertThat(filter.isFiltering()).isFalse();
        assertThat(filter.isVisible(getBookmark(bookmark1Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark2Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark3Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(folder1Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(folder2Id))).isTrue();
    }

    @Test
    public void testIsIdFiltering_falseByDefault() {
        // Then - no ID filter active initially
        assertThat(filter.isIdFiltering()).isFalse();

        // When
        filter.setIdFilter(Set.of(bookmark1Id));

        // Then - ID filtering is now active
        assertThat(filter.isIdFiltering()).isTrue();
    }

    @Test
    public void testClear_alsoResetsSearchText() {
        // Given - a text filter is active
        filter.setSearchText("Java");
        assertThat(filter.isFiltering()).isTrue();

        // When
        filter.clear();

        // Then - search text is reset and all bookmarks are visible
        assertThat(filter.isFiltering()).isFalse();
        assertThat(filter.getSearchText()).isEmpty();
        assertThat(filter.isVisible(getBookmark(bookmark1Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark2Id))).isTrue();
    }

    @Test
    public void testSetSearchText_clearsIdFilter() {
        // Given - an ID filter is active
        filter.setIdFilter(Set.of(bookmark3Id));
        assertThat(filter.isIdFiltering()).isTrue();

        // When - set a search text
        filter.setSearchText("Java");

        // Then - ID filter is cleared
        assertThat(filter.isIdFiltering()).isFalse();
        // And search text filter is active
        assertThat(filter.isFiltering()).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark1Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark3Id))).isTrue();
        assertThat(filter.isVisible(getBookmark(bookmark2Id))).isFalse();
    }

    private Bookmark getBookmark(BookmarkId bookmarkId) {
        return bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId);
    }
}

