package mesfavoris.internal.visited;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeGenerator;
import mesfavoris.tests.commons.bookmarks.IncrementalIDGenerator;
import mesfavoris.topics.BookmarksActivityListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static mesfavoris.tests.commons.bookmarks.BookmarksTreeTestUtil.getBookmark;
import static org.assertj.core.api.Assertions.assertThat;

public class VisitedBookmarksDatabaseTest extends BasePlatformTestCase {
	private VisitedBookmarksDatabase visitedBookmarksDatabase;
	private BookmarkDatabase bookmarkDatabase;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		BookmarksTree bookmarksTree = new BookmarksTreeGenerator(new IncrementalIDGenerator(), 5, 1, 5).build();
		bookmarkDatabase = new BookmarkDatabase("main", bookmarksTree);
		visitedBookmarksDatabase = new VisitedBookmarksDatabase(getProject(), bookmarkDatabase);
		visitedBookmarksDatabase.init();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		try {
			if (visitedBookmarksDatabase != null) {
				visitedBookmarksDatabase.close();
			}
		} finally {
			super.tearDown();
		}
	}

	@Test
	public void testVisitBookmark() {
		// Given
		BookmarkId bookmarkId = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 1).getId();

		// When
		bookmarkVisited(bookmarkId);
		bookmarkVisited(bookmarkId);

		// Then
		assertThat(visitedBookmarksDatabase.getVisitedBookmarks().getMostVisitedBookmarks(5)).containsExactly(bookmarkId);
	}

	@Test
	public void testGetMostVisitedBookmarks() {
		// Given
		BookmarkId bookmarkId1 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 1).getId();
		BookmarkId bookmarkId2 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 2).getId();
		BookmarkId bookmarkId3 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 3).getId();

		// When
		bookmarkVisited(bookmarkId1);
		bookmarkVisited(bookmarkId1);
		bookmarkVisited(bookmarkId3);
		bookmarkVisited(bookmarkId1);
		bookmarkVisited(bookmarkId2);
		bookmarkVisited(bookmarkId2);

		// Then
		assertThat(visitedBookmarksDatabase.getVisitedBookmarks().getMostVisitedBookmarks(5)).containsExactly(bookmarkId1,
				bookmarkId2, bookmarkId3);
	}

	@Test
	public void testGetLatestVisitedBookmarks() throws Exception {
		// Given
		BookmarkId bookmarkId1 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 1).getId();
		BookmarkId bookmarkId2 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 2).getId();
		BookmarkId bookmarkId3 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 3).getId();

		// When
		bookmarkVisited(bookmarkId1);
		Thread.sleep(50);
		bookmarkVisited(bookmarkId1);
		Thread.sleep(50);
		bookmarkVisited(bookmarkId3);
		Thread.sleep(50);
		bookmarkVisited(bookmarkId1);
		Thread.sleep(50);
		bookmarkVisited(bookmarkId2);
		Thread.sleep(50);
		bookmarkVisited(bookmarkId2);

		// Then
		assertThat(visitedBookmarksDatabase.getVisitedBookmarks().getLatestVisitedBookmarks(5)).containsExactly(bookmarkId2,
				bookmarkId1, bookmarkId3);
	}	
	
	@Test
	public void testBookmarkDeleted() throws Exception {
		// Given
		BookmarkId bookmarkId1 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 1).getId();
		BookmarkId bookmarkId2 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 2).getId();
		BookmarkId bookmarkId3 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 3).getId();
		bookmarkVisited(bookmarkId1);
		bookmarkVisited(bookmarkId1);
		bookmarkVisited(bookmarkId3);
		bookmarkVisited(bookmarkId1);
		bookmarkVisited(bookmarkId2);
		bookmarkVisited(bookmarkId2);

		// When
		bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier.deleteBookmark(bookmarkId1, false));

		// Then
		assertThat(visitedBookmarksDatabase.getVisitedBookmarks().getMostVisitedBookmarks(5)).containsExactly(bookmarkId2,
				bookmarkId3);
	}

	@Test
	public void testBookmarkFolderDeleted() throws Exception {
		// Given
		BookmarkId bookmarkFolderId = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0).getId();
		BookmarkId bookmarkId1 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 1).getId();
		BookmarkId bookmarkId2 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 1).getId();
		BookmarkId bookmarkId3 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 2).getId();
		bookmarkVisited(bookmarkId1);
		bookmarkVisited(bookmarkId1);
		bookmarkVisited(bookmarkId3);
		bookmarkVisited(bookmarkId1);
		bookmarkVisited(bookmarkId2);
		bookmarkVisited(bookmarkId2);

		// When
		bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier.deleteBookmark(bookmarkFolderId, true));

		// Then
		assertThat(visitedBookmarksDatabase.getVisitedBookmarks().getMostVisitedBookmarks(5)).containsExactly(bookmarkId2,
				bookmarkId3);
	}

	@Test
	public void testLoadSaveMostVisitedBookmarks() throws Exception {
		// Given
		BookmarkId bookmarkId1 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 1).getId();
		BookmarkId bookmarkId2 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 2).getId();
		BookmarkId bookmarkId3 = getBookmark(bookmarkDatabase.getBookmarksTree(), 0, 0, 3).getId();
		bookmarkVisited(bookmarkId1);
		bookmarkVisited(bookmarkId1);
		bookmarkVisited(bookmarkId3);
		bookmarkVisited(bookmarkId1);
		bookmarkVisited(bookmarkId2);
		bookmarkVisited(bookmarkId2);

		// When
		Thread.sleep(2000); // should not be necessary but test fails on assertThat on travis too often
		var state = visitedBookmarksDatabase.getState();
		visitedBookmarksDatabase.close();
		visitedBookmarksDatabase = new VisitedBookmarksDatabase(getProject(), bookmarkDatabase);
		visitedBookmarksDatabase.loadState(state);
		visitedBookmarksDatabase.init();

		// Then
		assertThat(visitedBookmarksDatabase.getVisitedBookmarks().getMostVisitedBookmarks(5)).containsExactly(bookmarkId1,
				bookmarkId2, bookmarkId3);
	}

	private void bookmarkVisited(BookmarkId bookmarkId) {
		getProject().getMessageBus().syncPublisher(BookmarksActivityListener.TOPIC).bookmarkVisited(bookmarkId);
	}

}
