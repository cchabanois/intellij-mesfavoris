package mesfavoris.internal.markers;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.editor.Document;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.IBookmarksService;
import mesfavoris.tests.commons.markers.BookmarkHighlightersTestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_LINE_NUMBER;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_WORKSPACE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for BookmarksHighlightersUtils
 */
public class BookmarksHighlightersUtilsTest extends BasePlatformTestCase {

    private IBookmarksService bookmarksService;
    private BookmarkDatabase bookmarkDatabase;
    private BookmarkId rootFolderId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        bookmarksService = getProject().getService(IBookmarksService.class);
        bookmarkDatabase = bookmarksService.getBookmarkDatabase();
        rootFolderId = bookmarkDatabase.getBookmarksTree().getRootFolder().getId();
    }

    @Test
    public void testFindBookmarksAtLineWithNoBookmarks() {
        // Given
        myFixture.configureByText("test.txt", "first line\nsecond line\nthird line");
        Document document = myFixture.getEditor().getDocument();

        // When
        List<BookmarkId> bookmarkIds = BookmarksHighlightersUtils.findBookmarksAtLine(getProject(), document, 0);

        // Then
        assertThat(bookmarkIds).isEmpty();
    }

    @Test
    public void testFindBookmarksAtLineWithSingleBookmark() throws Exception {
        // Given
        myFixture.configureByText("test.txt", "first line\nsecond line\nthird line");
        Document document = myFixture.getEditor().getDocument();

        BookmarkId bookmarkId = new BookmarkId();
        Bookmark bookmark = createBookmark(bookmarkId, "test.txt", 1);
        addBookmark(bookmark);
        BookmarkHighlightersTestUtils.waitUntilBookmarkHighlighterAtLine(getProject(), document, bookmarkId, 1);

        // When
        List<BookmarkId> bookmarkIds = BookmarksHighlightersUtils.findBookmarksAtLine(getProject(), document, 1);

        // Then
        assertThat(bookmarkIds).hasSize(1);
        assertThat(bookmarkIds.get(0)).isEqualTo(bookmarkId);
    }

    @Test
    public void testFindBookmarksAtLineWithMultipleBookmarks() throws Exception {
        // Given
        myFixture.configureByText("test.txt", "first line\nsecond line\nthird line");
        Document document = myFixture.getEditor().getDocument();

        BookmarkId bookmarkId1 = new BookmarkId();
        BookmarkId bookmarkId2 = new BookmarkId();
        Bookmark bookmark1 = createBookmark(bookmarkId1, "test.txt", 1);
        Bookmark bookmark2 = createBookmark(bookmarkId2, "test.txt", 1);

        addBookmark(bookmark1);
        addBookmark(bookmark2);
        BookmarkHighlightersTestUtils.waitUntilBookmarkHighlighterAtLine(getProject(), document, bookmarkId1, 1);
        BookmarkHighlightersTestUtils.waitUntilBookmarkHighlighterAtLine(getProject(), document, bookmarkId2, 1);

        // When
        List<BookmarkId> bookmarkIds = BookmarksHighlightersUtils.findBookmarksAtLine(getProject(), document, 1);

        // Then
        assertThat(bookmarkIds).hasSize(2);
        assertThat(bookmarkIds).containsExactlyInAnyOrder(bookmarkId1, bookmarkId2);
    }

    @Test
    public void testGetBookmarksAtCaretWithNoBookmarks() {
        // Given
        myFixture.configureByText("test.txt", "first line\nsecond line\nthird line");
        myFixture.getEditor().getCaretModel().moveToOffset(0);

        // When
        List<BookmarkId> bookmarkIds = BookmarksHighlightersUtils.getBookmarksAtCaret(getProject());

        // Then
        assertThat(bookmarkIds).isEmpty();
    }

    @Test
    public void testGetBookmarksAtCaretWithSingleBookmark() throws Exception {
        // Given
        myFixture.configureByText("test.txt", "first line\nsecond line\nthird line");
        Document document = myFixture.getEditor().getDocument();

        BookmarkId bookmarkId = new BookmarkId();
        Bookmark bookmark = createBookmark(bookmarkId, "test.txt", 1);
        addBookmark(bookmark);
        BookmarkHighlightersTestUtils.waitUntilBookmarkHighlighterAtLine(getProject(), document, bookmarkId, 1);

        // Position caret at line 1
        int line1Offset = document.getLineStartOffset(1);
        myFixture.getEditor().getCaretModel().moveToOffset(line1Offset);

        // When
        List<BookmarkId> bookmarkIds = BookmarksHighlightersUtils.getBookmarksAtCaret(getProject());

        // Then
        assertThat(bookmarkIds).hasSize(1);
        assertThat(bookmarkIds.get(0)).isEqualTo(bookmarkId);
    }

    @Test
    public void testGetBookmarksAtCaretWithMultipleBookmarks() throws Exception {
        // Given
        myFixture.configureByText("test.txt", "first line\nsecond line\nthird line");
        Document document = myFixture.getEditor().getDocument();

        BookmarkId bookmarkId1 = new BookmarkId();
        BookmarkId bookmarkId2 = new BookmarkId();
        Bookmark bookmark1 = createBookmark(bookmarkId1, "test.txt", 1);
        Bookmark bookmark2 = createBookmark(bookmarkId2, "test.txt", 1);

        addBookmark(bookmark1);
        addBookmark(bookmark2);
        BookmarkHighlightersTestUtils.waitUntilBookmarkHighlighterAtLine(getProject(), document, bookmarkId1, 1);
        BookmarkHighlightersTestUtils.waitUntilBookmarkHighlighterAtLine(getProject(), document, bookmarkId2, 1);

        // Position caret at line 1
        int line1Offset = document.getLineStartOffset(1);
        myFixture.getEditor().getCaretModel().moveToOffset(line1Offset);

        // When
        List<BookmarkId> bookmarkIds = BookmarksHighlightersUtils.getBookmarksAtCaret(getProject());

        // Then
        assertThat(bookmarkIds).hasSize(2);
        assertThat(bookmarkIds).containsExactlyInAnyOrder(bookmarkId1, bookmarkId2);
    }

    // Helper methods

    private Bookmark createBookmark(BookmarkId bookmarkId, String fileName, int lineNumber) {
        return new Bookmark(bookmarkId, ImmutableMap.of(
                "name", "Bookmark at line " + lineNumber,
                PROP_WORKSPACE_PATH, "//" + getProject().getName() + "/" + fileName,
                PROP_LINE_NUMBER, String.valueOf(lineNumber)
        ));
    }

    private void addBookmark(Bookmark bookmark) throws Exception {
        bookmarkDatabase.modify(bookmarksTreeModifier ->
                bookmarksTreeModifier.addBookmarks(rootFolderId, Collections.singletonList(bookmark)));
    }
}

