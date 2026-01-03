package mesfavoris.internal.service.operations;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.internal.toolwindow.BookmarksTreeComponent;
import mesfavoris.internal.toolwindow.MesFavorisToolWindowUtils;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.IBookmarksService;
import mesfavoris.tests.commons.toolwindow.MesFavorisToolWindowTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.tree.TreePath;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static org.assertj.core.api.Assertions.assertThat;

public class SelectBookmarkInTreeOperationTest extends BasePlatformTestCase {
    private SelectBookmarkInTreeOperation operation;
    private IBookmarksService bookmarksService;
    private BookmarkDatabase bookmarkDatabase;
    private BookmarkId bookmark1Id;
    private BookmarkId bookmark2Id;
    private BookmarkId folder1Id;
    private BookmarkId nestedBookmarkId;
    private ToolWindow toolWindow;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Get the bookmarks service and database
        bookmarksService = getProject().getService(IBookmarksService.class);
        bookmarkDatabase = bookmarksService.getBookmarkDatabase();

        // Create test bookmarks with unique IDs
        bookmark1Id = new BookmarkId();
        bookmark2Id = new BookmarkId();
        folder1Id = new BookmarkId();
        nestedBookmarkId = new BookmarkId();

        bookmarkDatabase.modify(modifier -> {
            modifier.addBookmarks(bookmarkDatabase.getBookmarksTree().getRootFolder().getId(),
                    java.util.Arrays.asList(
                            bookmarkFolder(folder1Id, "Folder 1").build(),
                            bookmark(bookmark1Id, "Bookmark 1").build(),
                            bookmark(bookmark2Id, "Bookmark 2").build()
                    ));
            // Add a nested bookmark inside folder1
            modifier.addBookmarks(folder1Id,
                    java.util.Collections.singletonList(
                            bookmark(nestedBookmarkId, "Nested Bookmark").build()
                    ));
        });

        // Register and setup tool window
        toolWindow = MesFavorisToolWindowTestUtils.setupMesfavorisToolWindow(getProject());

        // Create the operation
        operation = new SelectBookmarkInTreeOperation(getProject());
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSelectBookmarkInTree() {
        // Given
        assertThat(toolWindow).isNotNull();
        BookmarksTreeComponent tree = MesFavorisToolWindowUtils.findBookmarksTree(getProject());
        assertThat(tree).isNotNull();

        // When
        operation.selectBookmark(bookmark1Id);
        MesFavorisToolWindowTestUtils.waitUntilBookmarkSelected(getProject(), bookmark1Id);

        // Then
        TreePath selectedPath = tree.getSelectionPath();
        assertThat(selectedPath).isNotNull();
        assertThat(tree.getBookmark(selectedPath).getId()).isEqualTo(bookmark1Id);
    }

    @Test
    public void testSelectDifferentBookmark() {
        // Given
        assertThat(toolWindow).isNotNull();
        BookmarksTreeComponent tree = MesFavorisToolWindowUtils.findBookmarksTree(getProject());
        assertThat(tree).isNotNull();

        // When: Select first bookmark
        operation.selectBookmark(bookmark1Id);
        MesFavorisToolWindowTestUtils.waitUntilBookmarkSelected(getProject(), bookmark1Id);

        // Then: First bookmark is selected
        assertThat(tree.getSelectionPath()).isNotNull();
        assertThat(tree.getBookmark(tree.getSelectionPath()).getId()).isEqualTo(bookmark1Id);

        // When: Select second bookmark
        operation.selectBookmark(bookmark2Id);
        MesFavorisToolWindowTestUtils.waitUntilBookmarkSelected(getProject(), bookmark2Id);

        // Then: Second bookmark is selected
        TreePath selectedPath = tree.getSelectionPath();
        assertThat(selectedPath).isNotNull();
        assertThat(tree.getBookmark(selectedPath).getId()).isEqualTo(bookmark2Id);
    }

    @Test
    public void testSelectBookmarkFolder() {
        // Given
        assertThat(toolWindow).isNotNull();
        BookmarksTreeComponent tree = MesFavorisToolWindowUtils.findBookmarksTree(getProject());
        assertThat(tree).isNotNull();

        // When
        operation.selectBookmark(folder1Id);
        MesFavorisToolWindowTestUtils.waitUntilBookmarkSelected(getProject(), folder1Id);

        // Then
        TreePath selectedPath = tree.getSelectionPath();
        assertThat(selectedPath).isNotNull();
        assertThat(tree.getBookmark(selectedPath).getId()).isEqualTo(folder1Id);
    }

    @Test
    public void testSelectNonExistentBookmark() {
        // Given: Select a bookmark first to have an initial selection
        BookmarksTreeComponent tree = MesFavorisToolWindowUtils.findBookmarksTree(getProject());
        assertThat(tree).isNotNull();
        operation.selectBookmark(bookmark1Id);
        MesFavorisToolWindowTestUtils.waitUntilBookmarkSelected(getProject(), bookmark1Id);
        TreePath initialSelection = tree.getSelectionPath();
        assertThat(initialSelection).isNotNull();

        // When: Try to select a non-existent bookmark
        BookmarkId nonExistentId = new BookmarkId();
        assertThat(bookmarkDatabase.getBookmarksTree().getBookmark(nonExistentId)).isNull();
        operation.selectBookmark(nonExistentId);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        // Then: Selection should not change
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // Ignore
        }
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        assertThat(tree.getSelectionPath()).isEqualTo(initialSelection);
    }

    @Test
    public void testSelectNestedBookmark() {
        // Given
        assertThat(toolWindow).isNotNull();
        BookmarksTreeComponent tree = MesFavorisToolWindowUtils.findBookmarksTree(getProject());
        assertThat(tree).isNotNull();

        // When: Select a bookmark that is nested inside a folder
        operation.selectBookmark(nestedBookmarkId);
        MesFavorisToolWindowTestUtils.waitUntilBookmarkSelected(getProject(), nestedBookmarkId);

        // Then: The nested bookmark should be selected
        TreePath selectedPath = tree.getSelectionPath();
        assertThat(selectedPath).isNotNull();
        assertThat(tree.getBookmark(selectedPath).getId()).isEqualTo(nestedBookmarkId);

        // And: The path should have the correct depth (root -> folder -> bookmark)
        assertThat(selectedPath.getPathCount()).isEqualTo(3);
    }
}

