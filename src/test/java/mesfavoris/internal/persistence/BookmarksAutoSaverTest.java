package mesfavoris.internal.persistence;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder;
import mesfavoris.tests.commons.waits.Waiter;

import java.time.Duration;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class BookmarksAutoSaverTest extends BasePlatformTestCase {
    private BookmarksAutoSaver bookmarksAutoSaver;
    private final LocalBookmarksSaver localBookmarksSaver = mock(LocalBookmarksSaver.class);
    private final RemoteBookmarksSaver remoteBookmarksSaver = mock(RemoteBookmarksSaver.class);
    private BookmarkDatabase bookmarkDatabase;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        bookmarkDatabase = new BookmarkDatabase("test", getInitialTree());
        bookmarksAutoSaver = new BookmarksAutoSaver(getProject(), bookmarkDatabase, localBookmarksSaver, remoteBookmarksSaver);
        bookmarksAutoSaver.init();
    }

    @Override
    public void tearDown() throws Exception {
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        Disposer.dispose(bookmarksAutoSaver);
        super.tearDown();
    }

    public void testAutoSave() throws Exception {
        // When
        bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier
                .setPropertyValue(new BookmarkId("bookmark1"), Bookmark.PROPERTY_NAME, "bookmark1 renamed"));

        // Wait until the handler has processed the event
        Waiter.waitUntil("bookmarks not saved",
                () -> bookmarksAutoSaver.getBackgroundBookmarksModificationsHandler().getQueueSize() == 0,
                Duration.ofMillis(5000));

        // Then
        verify(localBookmarksSaver, atLeastOnce()).saveBookmarks(any(BookmarksTree.class));
    }

    public void testDirtyBookmarks() throws Exception {
        // When
        bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier
                .setPropertyValue(new BookmarkId("bookmark1"), Bookmark.PROPERTY_NAME, "bookmark1 renamed"));

        // Wait until the dirty bookmarks set is updated
        Waiter.waitUntil("bookmarks are dirty",
                () -> bookmarksAutoSaver.getDirtyBookmarks().contains(new BookmarkId("bookmark1")),
                Duration.ofMillis(5000));

        // Then
        assertThat(bookmarksAutoSaver.getDirtyBookmarks()).containsExactly(new BookmarkId("bookmark1"));
    }


    private BookmarksTree getInitialTree() {
        BookmarksTreeBuilder bookmarksTreeBuilder = bookmarksTree("rootFolder");
        bookmarksTreeBuilder.addBookmarks("rootFolder", bookmarkFolder("bookmarkFolder1"),
                bookmarkFolder("bookmarkFolder2"));
        bookmarksTreeBuilder.addBookmarks("bookmarkFolder1", bookmark("bookmark1"), bookmark("bookmark2"));
        bookmarksTreeBuilder.addBookmarks("bookmarkFolder2", bookmark("bookmark3"), bookmark("bookmark4"));
        return bookmarksTreeBuilder.build();
    }

}
