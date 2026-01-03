package mesfavoris.internal.service.operations;

import com.google.common.collect.Lists;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.internal.remote.InMemoryRemoteBookmarksStore;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksDirtyStateTracker;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.mockito.Mockito.*;

public class RefreshRemoteFolderOperationTest extends BasePlatformTestCase {
	private InMemoryRemoteBookmarksStore remoteBookmarksStore;
	private RemoteBookmarksStoreManager remoteBookmarksStoreManager;
	private RefreshRemoteFolderOperation refreshRemoteFolderOperation;
	private IBookmarksDirtyStateTracker bookmarksDirtyStateTracker = mock(IBookmarksDirtyStateTracker.class);
	private BookmarkDatabase bookmarkDatabase;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.remoteBookmarksStore = new InMemoryRemoteBookmarksStore(getProject());
		this.remoteBookmarksStoreManager = new RemoteBookmarksStoreManager(
				() -> Lists.newArrayList(remoteBookmarksStore));
		this.bookmarkDatabase = new BookmarkDatabase("testId", getInitialTree());
		this.refreshRemoteFolderOperation = new RefreshRemoteFolderOperation(bookmarkDatabase,
				remoteBookmarksStoreManager, bookmarksDirtyStateTracker);
	}

	public void testRefreshRemoteFolder() throws Exception {
		// Given
		remoteBookmarksStore.add(getRemoteBookmarkFolder2(), new BookmarkId("bookmarkFolder2"),
				new EmptyProgressIndicator());

		// When
		refreshRemoteFolderOperation.refresh(new BookmarkId("bookmarkFolder2"), new EmptyProgressIndicator());

		// Then
		assertEquals(getRemoteBookmarkFolder2().toString(),
				bookmarkDatabase.getBookmarksTree().subTree(new BookmarkId("bookmarkFolder2")).toString());
	}

	public void testRefreshAllRemoteFolders() throws Exception {
		// Given
		remoteBookmarksStore.add(getRemoteBookmarkFolder2(), new BookmarkId("bookmarkFolder2"),
				new EmptyProgressIndicator());
		remoteBookmarksStore.connect(new EmptyProgressIndicator());

		// When
		refreshRemoteFolderOperation.refresh(new EmptyProgressIndicator());

		// Then
		assertEquals(getRemoteBookmarkFolder2().toString(),
				bookmarkDatabase.getBookmarksTree().subTree(new BookmarkId("bookmarkFolder2")).toString());
	}

	public void testRefreshRemoteFolderWaitsUntilNotDirty() throws Exception {
		// Given
		remoteBookmarksStore.add(getRemoteBookmarkFolder2(), new BookmarkId("bookmarkFolder2"),
				new EmptyProgressIndicator());
		when(bookmarksDirtyStateTracker.isDirty()).thenReturn(true, true, true, false);

		// When
		refreshRemoteFolderOperation.refresh(new BookmarkId("bookmarkFolder2"), new EmptyProgressIndicator());

		// Then
		assertEquals(getRemoteBookmarkFolder2().toString(),
				bookmarkDatabase.getBookmarksTree().subTree(new BookmarkId("bookmarkFolder2")).toString());
		verify(bookmarksDirtyStateTracker, times(4)).isDirty();

	}

	private BookmarksTree getInitialTree() {
		BookmarksTreeBuilder bookmarksTreeBuilder = bookmarksTree("rootFolder");
		bookmarksTreeBuilder.addBookmarks("rootFolder", bookmarkFolder("bookmarkFolder1"),
				bookmarkFolder("bookmarkFolder2"));
		bookmarksTreeBuilder.addBookmarks("bookmarkFolder1", bookmark("bookmark1"), bookmark("bookmark2"));
		bookmarksTreeBuilder.addBookmarks("bookmarkFolder2", bookmark("bookmark3"), bookmark("bookmark4"));
		return bookmarksTreeBuilder.build();
	}

	private BookmarksTree getRemoteBookmarkFolder2() {
		BookmarksTreeBuilder bookmarksTreeBuilder = bookmarksTree("bookmarkFolder2");
		bookmarksTreeBuilder.addBookmarks("bookmarkFolder2", bookmark("bookmark3"), bookmark("bookmark4"),
				bookmark("bookmark5"));
		return bookmarksTreeBuilder.build();
	}

}
