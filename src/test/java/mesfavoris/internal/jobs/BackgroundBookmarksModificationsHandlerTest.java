package mesfavoris.internal.jobs;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.internal.jobs.BackgroundBookmarksModificationsHandler.IBookmarksModificationsHandler;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class BackgroundBookmarksModificationsHandlerTest extends BasePlatformTestCase {
    private BackgroundBookmarksModificationsHandler backgroundBookmarksModificationsHandler;
    private final IBookmarksModificationsHandler bookmarksModificationsHandler = mock(IBookmarksModificationsHandler.class);
    private BookmarkDatabase bookmarkDatabase;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.bookmarkDatabase = new BookmarkDatabase("testId", getInitialTree());
        backgroundBookmarksModificationsHandler = new BackgroundBookmarksModificationsHandler(getProject(), bookmarkDatabase, bookmarksModificationsHandler, 500);
        backgroundBookmarksModificationsHandler.init();
    }

    @Override
    protected void tearDown() throws Exception {
        Disposer.dispose(backgroundBookmarksModificationsHandler);
        super.tearDown();
    }

    public void testDoNotDelegateUntilDelay() throws Exception {
        // Given
        bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier.deleteBookmark(new BookmarkId("bookmark4"), false));
        Thread.sleep(300);
        bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier.deleteBookmark(new BookmarkId("bookmark3"), false));
        Thread.sleep(300);
        bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier.deleteBookmark(new BookmarkId("bookmark2"), false));
        ArgumentCaptor<List> anyList = ArgumentCaptor.forClass(List.class);

        // Then
        verify(bookmarksModificationsHandler, timeout(5000)).handle(anyList.capture(), any(ProgressIndicator.class));
        assertThat(anyList.getValue()).hasSize(3);
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
