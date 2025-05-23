package mesfavoris.internal.jobs;

import mesfavoris.internal.jobs.BackgroundBookmarksModificationsHandler.IBookmarksModificationsHandler;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BackgroundBookmarksModificationsHandlerTest {
	private BackgroundBookmarksModificationsHandler backgroundBookmarksModificationsHandler;
	private IBookmarksModificationsHandler bookmarksModificationsHandler = mock(IBookmarksModificationsHandler.class);
	private BookmarkDatabase bookmarkDatabase;
	
	@Before
	public void setUp() {
		this.bookmarkDatabase = new BookmarkDatabase("testId", getInitialTree());
		backgroundBookmarksModificationsHandler = new BackgroundBookmarksModificationsHandler(bookmarkDatabase, bookmarksModificationsHandler, 500);
		backgroundBookmarksModificationsHandler.init();
	}
	
	@After
	public void tearDown() {
		backgroundBookmarksModificationsHandler.close();
	}
	
	
	@Test
	public void testDoNotDelegateUntilDelay() throws Exception {
		// Given
		bookmarkDatabase.modify(bookmarksTreeModifier->bookmarksTreeModifier.deleteBookmark(new BookmarkId("bookmark4"), false));
		Thread.sleep(300);
		bookmarkDatabase.modify(bookmarksTreeModifier->bookmarksTreeModifier.deleteBookmark(new BookmarkId("bookmark3"), false));
		Thread.sleep(300);
		bookmarkDatabase.modify(bookmarksTreeModifier->bookmarksTreeModifier.deleteBookmark(new BookmarkId("bookmark2"), false));
		ArgumentCaptor<List> anyList = ArgumentCaptor.forClass(List.class);
		
		// When
		Thread.sleep(2000);
		
		// Then
		verify(bookmarksModificationsHandler).handle(anyList.capture());
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
