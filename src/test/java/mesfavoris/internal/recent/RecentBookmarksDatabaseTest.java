package mesfavoris.internal.recent;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.BookmarksException;
import mesfavoris.model.*;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static org.assertj.core.api.Assertions.assertThat;

public class RecentBookmarksDatabaseTest extends BasePlatformTestCase {
	private RecentBookmarksDatabase recentBookmarksDatabase;
	private BookmarkDatabase bookmarkDatabase;
	private final BookmarkId rootBookmarkId = new BookmarkId("root");
	private final Duration recentDuration = Duration.ofDays(1);

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(rootBookmarkId, "root"));
		bookmarkDatabase = new BookmarkDatabase("main", bookmarksTree);
		recentBookmarksDatabase = new RecentBookmarksDatabase(getProject(), bookmarkDatabase, recentDuration);
		recentBookmarksDatabase.init();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		try {
			if (recentBookmarksDatabase != null) {
				recentBookmarksDatabase.close();
			}
		} finally {
			super.tearDown();
		}
	}

	@Test
	public void testAddBookmark() throws Exception {
		// Given
		addBookmark(rootBookmarkId, "bookmark1", Instant.now());
		addBookmark(rootBookmarkId, "bookmark2", Instant.now());
		addBookmark(rootBookmarkId, bookmark("bookmark3").build());

		// When
		List<BookmarkId> recentBookmarks = recentBookmarksDatabase.getRecentBookmarks().getRecentBookmarks(10);

		// Then
		assertThat(recentBookmarks).containsExactly(new BookmarkId("bookmark2"), new BookmarkId("bookmark1"));
	}

	@Test
	public void testDeleteBookmark() throws Exception {
		// Given
		addBookmark(rootBookmarkId, "bookmark1", Instant.now());
		addBookmark(rootBookmarkId, "bookmark2", Instant.now());
		addBookmark(rootBookmarkId, "bookmark3", Instant.now());

		// When
		deleteBookmark(new BookmarkId("bookmark2"));
		List<BookmarkId> recentBookmarks = recentBookmarksDatabase.getRecentBookmarks().getRecentBookmarks(10);

		// Then
		assertThat(recentBookmarks).containsExactly(new BookmarkId("bookmark3"), new BookmarkId("bookmark1"));
	}

	@Test
	public void testDeleteBookmarkFolder() throws Exception {
		// Given
		addBookmark(rootBookmarkId, "bookmark1", Instant.now());
		BookmarkId bookmarkFolderId = new BookmarkId("bookmarkFolder");
		addBookmarkFolder(rootBookmarkId, "bookmarkFolder", Instant.now());
		addBookmark(bookmarkFolderId, "bookmark2", Instant.now());
		addBookmark(bookmarkFolderId, "bookmark3", Instant.now());

		// When
		deleteBookmark(new BookmarkId("bookmarkFolder"));
		List<BookmarkId> recentBookmarks = recentBookmarksDatabase.getRecentBookmarks().getRecentBookmarks(10);

		// Then
		assertThat(recentBookmarks).containsExactly(new BookmarkId("bookmark1"));
	}

	@Test
	public void testOnlyKeepRecentBookmarks() throws BookmarksException, InterruptedException {
		// Given
		addBookmark(rootBookmarkId, "bookmark1", Instant.now().minus(recentDuration).plus(Duration.ofSeconds(1)));
		assertThat(recentBookmarksDatabase.getRecentBookmarks().getRecentBookmarks(10))
				.containsExactly(new BookmarkId("bookmark1"));

		// When
		Thread.sleep(2000);
		addBookmark(rootBookmarkId, "bookmark2", Instant.now());
		List<BookmarkId> recentBookmarks = recentBookmarksDatabase.getRecentBookmarks().getRecentBookmarks(10);

		// Then
		assertThat(recentBookmarks).containsExactly(new BookmarkId("bookmark2"));
	}

	@Test
	public void testLoadSaveRecentBookmarks() throws Exception {
		// Given
		addBookmark(rootBookmarkId, "bookmark1", Instant.now());
		addBookmark(rootBookmarkId, "bookmark2", Instant.now());
		addBookmark(rootBookmarkId, "bookmark3", Instant.now());

		// When
        var state = recentBookmarksDatabase.getState();
		recentBookmarksDatabase.close();
		recentBookmarksDatabase = new RecentBookmarksDatabase(getProject(), bookmarkDatabase, recentDuration);
		recentBookmarksDatabase.loadState(state);
		recentBookmarksDatabase.init();

		// Then
		List<BookmarkId> recentBookmarks = recentBookmarksDatabase.getRecentBookmarks().getRecentBookmarks(10);
		assertThat(recentBookmarks).containsExactly(new BookmarkId("bookmark3"), new BookmarkId("bookmark2"), new BookmarkId("bookmark1"));
	}
	
	private void addBookmarkFolder(BookmarkId parentId, String name, Instant created) throws BookmarksException {
		BookmarkFolder bookmarkFolder = bookmarkFolder(name).created(created).build();
		addBookmark(parentId, bookmarkFolder);
	}

	private void addBookmark(BookmarkId parentId, String name, Instant created) throws BookmarksException {
		Bookmark bookmark = bookmark(name).created(created).build();
		addBookmark(parentId, bookmark);
	}

	private void addBookmark(BookmarkId parentId, Bookmark bookmark) throws BookmarksException {
		bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier.addBookmarks(parentId, Lists.newArrayList(bookmark)));
	}

	private void deleteBookmark(BookmarkId bookmarkId) throws BookmarksException {
		bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier.deleteBookmark(bookmarkId, true));
	}
}
