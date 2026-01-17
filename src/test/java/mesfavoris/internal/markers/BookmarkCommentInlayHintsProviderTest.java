package mesfavoris.internal.markers;

import com.google.common.collect.ImmutableMap;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.hints.InlayHintsSettings;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.InlayModel;
import com.intellij.psi.PsiFile;
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
 * Tests for BookmarkCommentInlayHintsProvider
 */
public class BookmarkCommentInlayHintsProviderTest extends BasePlatformTestCase {

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

        // Ensure BookmarksHighlighters service is initialized
        getProject().getService(BookmarksHighlighters.class);

        // Enable inlay hints for bookmark comments
        InlayHintsSettings settings = InlayHintsSettings.instance();
        settings.setEnabledGlobally(true);
    }

    @Test
    public void testInlayHintDisplayedForBookmarkWithComment() throws Exception {
        // Given
        PsiFile psiFile = myFixture.configureByText("test.txt", "first line\nsecond line\nthird line");
        Document document = myFixture.getEditor().getDocument();

        BookmarkId bookmarkId = new BookmarkId();
        Bookmark bookmark = createBookmarkWithComment(bookmarkId, "test.txt", 1, "This is a comment");
        addBookmark(bookmark);
        BookmarkHighlightersTestUtils.waitUntilBookmarkHighlighterAtLine(getProject(), document, bookmarkId, 1);

        // When
        triggerInlayHintsUpdate(psiFile);

        // Then
        List<Inlay<?>> inlays = getBlockInlaysAtLine(document, 1);
        assertThat(inlays).isNotEmpty();
        String inlayText = inlays.get(0).toString();
        assertThat(inlayText).contains("This is a comment");
    }

    @Test
    public void testNoInlayHintForBookmarkWithoutComment() throws Exception {
        // Given
        PsiFile psiFile = myFixture.configureByText("test.txt", "first line\nsecond line\nthird line");
        Document document = myFixture.getEditor().getDocument();

        BookmarkId bookmarkId = new BookmarkId();
        Bookmark bookmark = createBookmark(bookmarkId, "test.txt", 1);
        addBookmark(bookmark);
        BookmarkHighlightersTestUtils.waitUntilBookmarkHighlighterAtLine(getProject(), document, bookmarkId, 1);

        // When
        triggerInlayHintsUpdate(psiFile);

        // Then
        List<Inlay<?>> inlays = getBlockInlaysAtLine(document, 1);
        assertThat(inlays).isEmpty();
    }

    @Test
    public void testNoInlayHintForBookmarkWithEmptyComment() throws Exception {
        // Given
        PsiFile psiFile = myFixture.configureByText("test.txt", "first line\nsecond line\nthird line");
        Document document = myFixture.getEditor().getDocument();

        BookmarkId bookmarkId = new BookmarkId();
        Bookmark bookmark = createBookmarkWithComment(bookmarkId, "test.txt", 1, "   ");
        addBookmark(bookmark);
        BookmarkHighlightersTestUtils.waitUntilBookmarkHighlighterAtLine(getProject(), document, bookmarkId, 1);

        // When
        triggerInlayHintsUpdate(psiFile);

        // Then
        List<Inlay<?>> inlays = getBlockInlaysAtLine(document, 1);
        assertThat(inlays).isEmpty();
    }

    @Test
    public void testMultilineCommentCreatesInlayWithMultipleLines() throws Exception {
        // Given
        PsiFile psiFile = myFixture.configureByText("test.txt", "first line\nsecond line\nthird line");
        Document document = myFixture.getEditor().getDocument();

        BookmarkId bookmarkId = new BookmarkId();
        Bookmark bookmark = createBookmarkWithComment(bookmarkId, "test.txt", 1, "Line 1\nLine 2\nLine 3");
        addBookmark(bookmark);
        BookmarkHighlightersTestUtils.waitUntilBookmarkHighlighterAtLine(getProject(), document, bookmarkId, 1);

        // When
        triggerInlayHintsUpdate(psiFile);

        // Then
        // Multiple lines in a comment are displayed in a single inlay with multiple lines
        List<Inlay<?>> inlays = getBlockInlaysAtLine(document, 1);
        assertThat(inlays).isNotEmpty();
        String inlayText = inlays.get(0).toString();
        assertThat(inlayText).contains("Line 1");
        assertThat(inlayText).contains("Line 2");
        assertThat(inlayText).contains("Line 3");
    }

    @Test
    public void testMultipleBookmarksOnSameLineShowAllComments() throws Exception {
        // Given
        PsiFile psiFile = myFixture.configureByText("test.txt", "first line\nsecond line\nthird line");
        Document document = myFixture.getEditor().getDocument();

        BookmarkId bookmarkId1 = new BookmarkId();
        BookmarkId bookmarkId2 = new BookmarkId();
        Bookmark bookmark1 = createBookmarkWithComment(bookmarkId1, "test.txt", 1, "Comment 1");
        Bookmark bookmark2 = createBookmarkWithComment(bookmarkId2, "test.txt", 1, "Comment 2");

        addBookmark(bookmark1);
        addBookmark(bookmark2);
        BookmarkHighlightersTestUtils.waitUntilBookmarkHighlighterAtLine(getProject(), document, bookmarkId1, 1);
        BookmarkHighlightersTestUtils.waitUntilBookmarkHighlighterAtLine(getProject(), document, bookmarkId2, 1);

        // When
        triggerInlayHintsUpdate(psiFile);

        // Then
        // Multiple bookmarks on the same line are displayed in a single inlay with multiple lines
        List<Inlay<?>> inlays = getBlockInlaysAtLine(document, 1);
        assertThat(inlays).isNotEmpty();
        String inlayText = inlays.get(0).toString();
        assertThat(inlayText).contains("Comment 1");
        assertThat(inlayText).contains("Comment 2");
    }

    // Helper methods

    private Bookmark createBookmark(BookmarkId bookmarkId, String fileName, int lineNumber) {
        return new Bookmark(bookmarkId, ImmutableMap.of(
                "name", "Bookmark at line " + lineNumber,
                PROP_WORKSPACE_PATH, "//" + getProject().getName() + "/" + fileName,
                PROP_LINE_NUMBER, String.valueOf(lineNumber)
        ));
    }

    private Bookmark createBookmarkWithComment(BookmarkId bookmarkId, String fileName, int lineNumber, String comment) {
        return new Bookmark(bookmarkId, ImmutableMap.of(
                "name", "Bookmark at line " + lineNumber,
                PROP_WORKSPACE_PATH, "//" + getProject().getName() + "/" + fileName,
                PROP_LINE_NUMBER, String.valueOf(lineNumber),
                Bookmark.PROPERTY_COMMENT, comment
        ));
    }

    private void addBookmark(Bookmark bookmark) throws Exception {
        bookmarkDatabase.modify(bookmarksTreeModifier ->
                bookmarksTreeModifier.addBookmarks(rootFolderId, Collections.singletonList(bookmark)));
    }

    private void triggerInlayHintsUpdate(PsiFile psiFile) {
        DaemonCodeAnalyzer.getInstance(getProject()).restart(psiFile);
        myFixture.doHighlighting();
    }

    private List<Inlay<?>> getBlockInlaysAtLine(Document document, int lineNumber) {
        InlayModel inlayModel = myFixture.getEditor().getInlayModel();
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        return inlayModel.getBlockElementsInRange(lineStartOffset, lineStartOffset);
    }
}

