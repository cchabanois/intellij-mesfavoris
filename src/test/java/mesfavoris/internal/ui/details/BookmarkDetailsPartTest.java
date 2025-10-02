package mesfavoris.internal.ui.details;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import mesfavoris.BookmarksException;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.snippets.internal.SnippetBookmarkDetailPart;
import mesfavoris.tests.commons.ui.ComponentFinder;
import mesfavoris.tests.commons.waits.Waiter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static mesfavoris.model.Bookmark.PROPERTY_COMMENT;
import static mesfavoris.model.Bookmark.PROPERTY_NAME;
import static mesfavoris.snippets.SnippetBookmarkProperties.PROP_SNIPPET_CONTENT;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;

public class BookmarkDetailsPartTest extends BasePlatformTestCase {
    private BookmarkDetailsPart bookmarkDetailsPart;
    private BookmarkDatabase bookmarkDatabase;
    private Disposable testDisposable;
    private JComponent component;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        testDisposable = Disposer.newDisposable();

        // Create a test bookmark database
        BookmarksTree bookmarksTree = bookmarksTree("root").build();
        bookmarkDatabase = new BookmarkDatabase("test", bookmarksTree);

        // Create the bookmark detail parts we want to test with our custom BookmarkDatabase
        List<mesfavoris.ui.details.IBookmarkDetailPart> detailParts = List.of(
                new CommentBookmarkDetailPart(getProject(), bookmarkDatabase),
                new SnippetBookmarkDetailPart(getProject(), bookmarkDatabase),
                new BookmarkPropertiesDetailPart(getProject(), bookmarkDatabase)
        );

        bookmarkDetailsPart = new BookmarkDetailsPart(getProject(), bookmarkDatabase, detailParts, testDisposable);
        bookmarkDetailsPart.init();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        try {
            if (testDisposable != null) {
                Disposer.dispose(testDisposable);
            }
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void testBookmarkWithCommentSelected() throws Exception {
        // Given
        Bookmark bookmark = new Bookmark(new BookmarkId(),
                ImmutableMap.of(PROPERTY_NAME, "my bookmark", PROPERTY_COMMENT, "my comment"));
        addBookmark(bookmarkDatabase.getBookmarksTree().getRootFolder().getId(), bookmark);

        // When
        component = bookmarkDetailsPart.createComponent();
        bookmarkDetailsPart.setBookmark(bookmark);

        // Then
        assertThat(getTabCount()).isEqualTo(2);
        waitUntilEditorTextTabHasContent("Comments", "my comment");
        verifyPropertiesTabHasBookmarkProperties(bookmark);
    }

    @Test
    public void testBookmarkWithSnippetSelected() throws BookmarksException, TimeoutException {
        // Given
        Bookmark bookmark = new Bookmark(new BookmarkId(),
                ImmutableMap.of(PROPERTY_NAME, "my bookmark", PROP_SNIPPET_CONTENT, "my snippet"));
        addBookmark(bookmarkDatabase.getBookmarksTree().getRootFolder().getId(), bookmark);

        // When
        component = bookmarkDetailsPart.createComponent();
        bookmarkDetailsPart.setBookmark(bookmark);

        // Then
        assertThat(getTabCount()).isEqualTo(3);
        waitUntilEditorTextTabHasContent("Snippet", "my snippet");
        verifyPropertiesTabHasBookmarkProperties(bookmark);
    }

    @Test
    public void testDeleteSelectedBookmark() throws BookmarksException, TimeoutException {
        // Given
        Bookmark bookmark = new Bookmark(new BookmarkId(), ImmutableMap.of(PROPERTY_NAME, "my bookmark",
                PROPERTY_COMMENT, "my comment", PROP_SNIPPET_CONTENT, "my snippet"));
        addBookmark(bookmarkDatabase.getBookmarksTree().getRootFolder().getId(), bookmark);
        component = bookmarkDetailsPart.createComponent();
        bookmarkDetailsPart.setBookmark(bookmark);
        assertThat(getTabCount()).isEqualTo(3);

        // When
        deleteBookmark(bookmark.getId());

        // Then
        assertThat(getTabCount()).isEqualTo(3);
        waitUntilEditorTextTabHasContent("Comments", "");
        waitUntilEditorTextTabHasContent("Snippet", "");
        waitUntilPropertiesTabIsEmpty();
    }




    private TabInfo getTabInfo(JBTabs tabs, String title) {
        for (TabInfo tabInfo : tabs.getTabs()) {
            if (title.equals(tabInfo.getText())) {
                return tabInfo;
            }
        }
        return null;
    }

    private int getTabCount() {
        JBTabs tabs = ComponentFinder.findChildComponent(component, JBTabs.class);
        return tabs != null ? tabs.getTabCount() : 0;
    }

    private void waitUntilPropertiesTabIsEmpty() throws TimeoutException {
        JBTabs tabs = ComponentFinder.findChildComponent(component, JBTabs.class);
        TabInfo propertiesTab = getTabInfo(tabs, "Properties");

        JComponent tabComponent = propertiesTab.getComponent();
        JBTable table = ComponentFinder.findChildComponent(tabComponent, JBTable.class);

        Waiter.waitUntil("Properties tab should be empty", () -> {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            return table.getRowCount() == 0;
        });
    }

    private void waitUntilEditorTextTabHasContent(String tabTitle, String expectedContent) throws TimeoutException {
        JBTabs tabs = ComponentFinder.findChildComponent(component, JBTabs.class);
        TabInfo tabInfo = getTabInfo(tabs, tabTitle);
        assertThat(tabInfo).isNotNull();

        EditorTextField editorField = ComponentFinder.findChildComponent(tabInfo.getComponent(), EditorTextField.class);
        assertThat(editorField).isNotNull();

        // Wait for the content to be updated (async operation)
        Waiter.waitUntil("EditorTextField should contain expected content: '" + expectedContent + "'", () -> {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            String currentText = editorField.getText();
            return expectedContent.equals(currentText);
        });
    }

    private void verifyPropertiesTabHasBookmarkProperties(Bookmark bookmark) {
        JBTabs tabs = ComponentFinder.findChildComponent(component, JBTabs.class);
        TabInfo propertiesTab = getTabInfo(tabs, "Properties");
        assertThat(propertiesTab).isNotNull();

        JComponent tabComponent = propertiesTab.getComponent();
        JBTable table = ComponentFinder.findChildComponent(tabComponent, JBTable.class);
        assertThat(table).isNotNull();

        // Verify that the table contains all bookmark properties
        int rowCount = table.getRowCount();
        assertThat(rowCount).isEqualTo(bookmark.getProperties().size());

        // Verify each property is in the table
        for (int row = 0; row < rowCount; row++) {
            String propertyName = (String) table.getValueAt(row, 0);
            String propertyValue = (String) table.getValueAt(row, 1);

            assertThat(bookmark.getProperties()).containsKey(propertyName);
            assertThat(bookmark.getProperties().get(propertyName)).isEqualTo(propertyValue);
        }
    }

    private void addBookmark(BookmarkId parentId, Bookmark bookmark) throws BookmarksException {
        bookmarkDatabase.modify(bookmarksTreeModifier ->
                bookmarksTreeModifier.addBookmarks(parentId, List.of(bookmark))
        );
    }

    private void deleteBookmark(BookmarkId bookmarkId) throws BookmarksException {
        bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier.deleteBookmark(bookmarkId, false));
    }

}
