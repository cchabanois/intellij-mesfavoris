package mesfavoris.internal.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.tree.TreePath;
import java.util.Collections;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;

import mesfavoris.tests.commons.waits.Waiter;

public class BookmarksTreeComponentTest extends BasePlatformTestCase {
    private BookmarkDatabase bookmarkDatabase;
    private BookmarksTreeComponent treeComponent;
    private Disposable testDisposable;
    private BookmarkId testBookmarkId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        testDisposable = Disposer.newDisposable();

        // Create a simple bookmarks tree using the builder
        testBookmarkId = new BookmarkId("test-bookmark");
        BookmarksTree bookmarksTree = bookmarksTree("root")
                .addBookmarks("root", bookmark(testBookmarkId, "Original Name"))
                .build();

        bookmarkDatabase = new BookmarkDatabase("test", bookmarksTree);
        treeComponent = new BookmarksTreeComponent(bookmarkDatabase, testDisposable);
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
    public void testConvertValueToTextAfterRename() throws Exception {
        // Given: Get the tree path for our test bookmark
        TreePath treePath = treeComponent.getTreePathForBookmark(testBookmarkId).orElseThrow();

        // When: Rename the bookmark in the database
        bookmarkDatabase.modify(modifier ->
            modifier.setPropertyValue(testBookmarkId, Bookmark.PROPERTY_NAME, "New Name"));

        // Then: Wait for the tree model to process the change and verify the result
        String displayText = Waiter.waitUntil("convertValueToText should return the new name", () -> {
            TreePath updatedTreePath = treeComponent.getTreePathForBookmark(testBookmarkId).orElseThrow();
            Bookmark updatedBookmark = treeComponent.getBookmark(updatedTreePath);
            String result = treeComponent.convertValueToText(updatedBookmark, false, false, true, 0, false);
            return "New Name".equals(result) ? result : null;
        });

        assertThat(displayText).isEqualTo("New Name");
    }

    @Test
    public void testConvertValueToTextWithUnnamedBookmark() throws Exception {
        // Given: Create a bookmark without a name property and add it to the database
        BookmarkId noNameBookmarkId = new BookmarkId("no-name");
        Bookmark noNameBookmark = new Bookmark(noNameBookmarkId); // This creates a bookmark without any properties
        bookmarkDatabase.modify(modifier ->
            modifier.addBookmarks(bookmarkDatabase.getBookmarksTree().getRootFolder().getId(),
                                Collections.singletonList(noNameBookmark)));

        // When: Call convertValueToText
        String displayText = treeComponent.convertValueToText(noNameBookmark, false, false, true, 0, false);

        // Then: Should return "unnamed"
        assertThat(displayText).isEqualTo("unnamed");
    }

    @Test
    public void testConvertValueToTextWithNonBookmarkObject() {
        // Given: A non-bookmark object
        String nonBookmarkValue = "not a bookmark";

        // When: Call convertValueToText
        String displayText = treeComponent.convertValueToText(nonBookmarkValue, false, false, true, 0, false);

        // Then: Should return the string representation
        assertThat(displayText).isEqualTo("not a bookmark");
    }
}
