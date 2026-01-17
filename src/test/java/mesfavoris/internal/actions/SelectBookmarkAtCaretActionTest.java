package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.internal.toolwindow.BookmarksTreeComponent;
import mesfavoris.internal.toolwindow.MesFavorisToolWindowUtils;
import mesfavoris.markers.IBookmarksHighlighters;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.IBookmarksService;
import mesfavoris.tests.commons.markers.BookmarkHighlightersTestUtils;
import mesfavoris.tests.commons.toolwindow.MesFavorisToolWindowTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.tree.TreePath;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_LINE_NUMBER;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_WORKSPACE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SelectBookmarkAtCaretActionTest extends BasePlatformTestCase {

    private IBookmarksService bookmarksService;
    private BookmarkDatabase bookmarkDatabase;
    private IBookmarksHighlighters bookmarksHighlighters;
    private SelectBookmarkAtCaretAction action;
    private ToolWindow toolWindow;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Get the bookmarks service and database
        bookmarksService = getProject().getService(IBookmarksService.class);
        bookmarkDatabase = bookmarksService.getBookmarkDatabase();
        bookmarksHighlighters = getProject().getService(IBookmarksHighlighters.class);

        // Create the action
        action = new SelectBookmarkAtCaretAction();

        // Setup tool window
        toolWindow = MesFavorisToolWindowTestUtils.setupMesfavorisToolWindow(getProject());
        assertThat(toolWindow).isNotNull();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        try {
            if (toolWindow != null) {
                ToolWindowManager.getInstance(getProject()).unregisterToolWindow("mesfavoris");
            }
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void testActionPerformedSelectsBookmarkInTree() throws Exception {
        // Given: Create a file with content and a bookmark at line 1
        myFixture.configureByText("test.txt", "first line\nsecond line\nthird line\nfourth line");

        BookmarkId bookmarkId = new BookmarkId();
        Map<String, String> properties = new HashMap<>();
        properties.put(Bookmark.PROPERTY_NAME, "Test Bookmark");
        properties.put(PROP_WORKSPACE_PATH, "test.txt");
        properties.put(PROP_LINE_NUMBER, "1");
        Bookmark bookmark = new Bookmark(bookmarkId, properties);

        bookmarkDatabase.modify(modifier -> modifier.addBookmarks(bookmarkDatabase.getBookmarksTree().getRootFolder().getId(),
                java.util.Collections.singletonList(bookmark)));

        // Position caret at line 1
        Document document = myFixture.getEditor().getDocument();
        int line1Offset = document.getLineStartOffset(1);
        myFixture.getEditor().getCaretModel().moveToOffset(line1Offset);

        // Wait for the highlighter to be created
        waitUntilBookmarkHighlighterAtLine(bookmarkId, 1);

        // When: Perform the action
        AnActionEvent event = createActionEvent();
        action.actionPerformed(event);

        // Then: The bookmark should be selected in the tree
        MesFavorisToolWindowTestUtils.waitUntilBookmarkSelected(getProject(), bookmarkId);

        BookmarksTreeComponent tree = MesFavorisToolWindowUtils.findBookmarksTree(getProject());
        assertThat(tree).isNotNull();
        TreePath selectedPath = tree.getSelectionPath();
        assertThat(selectedPath).isNotNull();
        assertThat(tree.getBookmark(selectedPath).getId()).isEqualTo(bookmarkId);
    }

    @Test
    public void testUpdateEnablesActionWhenBookmarkAtCaret() throws Exception {
        // Given: Create a file with content and a bookmark at line 1
        myFixture.configureByText("test.txt", "first line\nsecond line\nthird line\nfourth line");

        BookmarkId bookmarkId = new BookmarkId();
        Map<String, String> properties = new HashMap<>();
        properties.put(Bookmark.PROPERTY_NAME, "Test Bookmark");
        properties.put(PROP_WORKSPACE_PATH, "test.txt");
        properties.put(PROP_LINE_NUMBER, "1");
        Bookmark bookmark = new Bookmark(bookmarkId, properties);

        bookmarkDatabase.modify(modifier -> modifier.addBookmarks(bookmarkDatabase.getBookmarksTree().getRootFolder().getId(),
                java.util.Collections.singletonList(bookmark)));

        // Position caret at line 1
        Document document = myFixture.getEditor().getDocument();
        int line1Offset = document.getLineStartOffset(1);
        myFixture.getEditor().getCaretModel().moveToOffset(line1Offset);

        // Wait for the highlighter to be created
        waitUntilBookmarkHighlighterAtLine(bookmarkId, 1);

        // When: Update the action
        AnActionEvent event = createActionEvent();
        action.update(event);

        // Then: The action should be enabled and visible
        assertThat(event.getPresentation().isEnabledAndVisible()).isTrue();
    }

    @Test
    public void testUpdateDisablesActionWhenNoBookmarkAtCaret() throws Exception {
        // Given: Create a file with content and a bookmark at line 1
        myFixture.configureByText("test.txt", "first line\nsecond line\nthird line\nfourth line");

        BookmarkId bookmarkId = new BookmarkId();
        Map<String, String> properties = new HashMap<>();
        properties.put(Bookmark.PROPERTY_NAME, "Test Bookmark");
        properties.put(PROP_WORKSPACE_PATH, "test.txt");
        properties.put(PROP_LINE_NUMBER, "1");
        Bookmark bookmark = new Bookmark(bookmarkId, properties);

        bookmarkDatabase.modify(modifier -> modifier.addBookmarks(bookmarkDatabase.getBookmarksTree().getRootFolder().getId(),
                java.util.Collections.singletonList(bookmark)));

        // Position caret at line 0 (no bookmark there)
        Document document = myFixture.getEditor().getDocument();
        int line0Offset = document.getLineStartOffset(0);
        myFixture.getEditor().getCaretModel().moveToOffset(line0Offset);

        // Wait for the highlighter to be created
        waitUntilBookmarkHighlighterAtLine(bookmarkId, 1);

        // When: Update the action
        AnActionEvent event = createActionEvent();
        action.update(event);

        // Then: The action should be disabled and invisible
        assertThat(event.getPresentation().isEnabledAndVisible()).isFalse();
    }

    private AnActionEvent createActionEvent() {
        AnActionEvent event = mock(AnActionEvent.class);
        when(event.getProject()).thenReturn(getProject());
        when(event.getPresentation()).thenReturn(new Presentation());

        return event;
    }

    private void waitUntilBookmarkHighlighterAtLine(BookmarkId bookmarkId, int lineNumber) throws TimeoutException {
        BookmarkHighlightersTestUtils.waitUntilBookmarkHighlighterAtLine(getProject(), myFixture.getEditor().getDocument(), bookmarkId, lineNumber);
    }
}
