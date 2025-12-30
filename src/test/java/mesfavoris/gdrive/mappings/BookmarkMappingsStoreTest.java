package mesfavoris.gdrive.mappings;

import com.google.common.collect.ImmutableMap;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder;

import java.util.Collections;

import static mesfavoris.gdrive.mappings.BookmarkMapping.PROP_BOOKMARKS_COUNT;
import static mesfavoris.gdrive.mappings.BookmarkMapping.PROP_SHARING_USER;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BookmarkMappingsStoreTest extends BasePlatformTestCase {
	private BookmarkMappingsStore bookmarkMappingsStore;
	private IBookmarkMappingsListener listener;
	private BookmarkDatabase bookmarkDatabase;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		listener = mock(IBookmarkMappingsListener.class);
		bookmarkMappingsStore = new BookmarkMappingsStore(getProject());
		getProject().getMessageBus().connect().subscribe(IBookmarkMappingsListener.TOPIC, listener);
		bookmarkDatabase = new BookmarkDatabase("test", createBookmarksTree());
		bookmarkDatabase.addListener(bookmarkMappingsStore);
	}

	private BookmarksTree createBookmarksTree() {
		BookmarksTreeBuilder bookmarksTreeBuilder = bookmarksTree("root");
		bookmarksTreeBuilder.addBookmarks("root", bookmarkFolder("folder1"), bookmarkFolder("folder2"));
		bookmarksTreeBuilder.addBookmarks("folder1", bookmarkFolder("folder11"), bookmark("bookmark11"));

		return bookmarksTreeBuilder.build();
	}

	public void testAddMapping() throws Exception {
		// Given
		BookmarkId bookmarkFolderId = new BookmarkId("folder1");

		// When
		bookmarkMappingsStore.add(bookmarkFolderId, "fileId", ImmutableMap.of(PROP_SHARING_USER, "Cedric Chabanois"));

		// Then
		verify(listener).mappingAdded(bookmarkFolderId);
		assertEquals("fileId", bookmarkMappingsStore.getMapping(bookmarkFolderId).get().getFileId());
		assertEquals(bookmarkFolderId, bookmarkMappingsStore.getMapping("fileId").get().getBookmarkFolderId());
		assertThat(bookmarkMappingsStore.getMapping(bookmarkFolderId).get().getProperties())
				.containsEntry(PROP_SHARING_USER, "Cedric Chabanois");
	}

	public void testUpdateMapping() {
		// Given
		BookmarkId bookmarkFolderId = new BookmarkId("folder1");
		bookmarkMappingsStore.add(bookmarkFolderId, "fileId", ImmutableMap.of(PROP_SHARING_USER, "Cedric Chabanois"));

		// When
		bookmarkMappingsStore.update("fileId",
				ImmutableMap.of(PROP_SHARING_USER, "Cedric Chabanois", PROP_BOOKMARKS_COUNT, "10"));

		assertThat(bookmarkMappingsStore.getMapping(bookmarkFolderId).get().getProperties())
				.contains(entry(PROP_SHARING_USER, "Cedric Chabanois"), entry(PROP_BOOKMARKS_COUNT, "10"));
	}

	public void testMappingRemovedIfFolderDeleted() throws Exception {
		// Given
		BookmarkId bookmarkFolderId = new BookmarkId("folder1");
		bookmarkMappingsStore.add(bookmarkFolderId, "fileId", Collections.emptyMap());

		// When
		bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier.deleteBookmark(bookmarkFolderId, true));

		// Then
		assertFalse(bookmarkMappingsStore.getMapping("fileId").isPresent());
	}

	public void testMappingRemovedIfParentFolderDeleted() throws Exception {
		// Given
		bookmarkMappingsStore.add(new BookmarkId("folder11"), "file11Id", Collections.emptyMap());
		bookmarkMappingsStore.add(new BookmarkId("folder2"), "file2Id", Collections.emptyMap());

		// When
		bookmarkDatabase
				.modify(bookmarksTreeModifier -> bookmarksTreeModifier.deleteBookmark(new BookmarkId("folder1"), true));

		// Then
		assertFalse(bookmarkMappingsStore.getMapping("file11Id").isPresent());
		assertTrue(bookmarkMappingsStore.getMapping("file2Id").isPresent());
	}

}
