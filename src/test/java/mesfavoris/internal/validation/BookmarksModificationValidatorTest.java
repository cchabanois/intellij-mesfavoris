package mesfavoris.internal.validation;

import com.google.common.collect.Lists;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.commons.Status;
import mesfavoris.internal.Constants;
import mesfavoris.internal.remote.InMemoryRemoteBookmarksStore;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.modification.BookmarksModification;
import mesfavoris.model.modification.BookmarksTreeModifier;
import mesfavoris.remote.RemoteBookmarkFolder;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder;

import java.io.IOException;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;

public class BookmarksModificationValidatorTest extends BasePlatformTestCase {
	private InMemoryRemoteBookmarksStore remoteBookmarksStore;
	private BookmarksModificationValidator bookmarksModificationValidator;
	private RemoteBookmarksStoreManager remoteBookmarksStoreManager;
	private BookmarksTreeModifier bookmarksTreeModifier;
	private BookmarksTree bookmarksTree;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.bookmarksTree = createBookmarksTree();
		this.remoteBookmarksStore = new InMemoryRemoteBookmarksStore(getProject());
		this.remoteBookmarksStoreManager = new RemoteBookmarksStoreManager(
				() -> Lists.newArrayList(remoteBookmarksStore));
		bookmarksModificationValidator = new BookmarksModificationValidator(remoteBookmarksStoreManager);
		this.bookmarksTreeModifier = new BookmarksTreeModifier(bookmarksTree);
	}

	public void testCannotAddBookmarkInsideRemoteBookmarkFolderIfNotConnected() throws Exception {
		// Given
		addToRemoteBookmarksStore(new BookmarkId("folder1"));

		// When
		bookmarksTreeModifier.addBookmarks(new BookmarkId("folder11"),
				Lists.newArrayList(bookmark("bookmark111").build()));

		// Then
		Status status = validateModifications();
		assertThat(status.isOk()).isFalse();
	}

	public void testCanAddBookmarkInsideRemoteBookmarkFolderIfConnected() throws Exception {
		// Given
		addToRemoteBookmarksStore(new BookmarkId("folder1"));
		remoteBookmarksStore.connect(new EmptyProgressIndicator());

		// When
		bookmarksTreeModifier.addBookmarks(new BookmarkId("folder11"),
				Lists.newArrayList(bookmark("bookmark111").build()));

		// Then
		Status status = validateModifications();
		assertThat(status.isOk()).isTrue();
	}

	public void testCannotAddBookmarkInsideRemoteBookmarkFolderIfReadOnly() throws Exception {
		// Given
		addToRemoteBookmarksStore(new BookmarkId("folder1"));
		remoteBookmarksStore.addRemoteBookmarkFolderProperty(new BookmarkId("folder1"),
				RemoteBookmarkFolder.PROP_READONLY, Boolean.TRUE.toString());
		remoteBookmarksStore.connect(new EmptyProgressIndicator());

		// When
		bookmarksTreeModifier.addBookmarks(new BookmarkId("folder11"),
				Lists.newArrayList(bookmark("bookmark111").build()));

		// Then
		Status status = validateModifications();
		assertThat(status.isOk()).isFalse();
	}

	public void testCannotMoveRemoteBookmarkFolderInsideAnotherRemoteBookmarkFolder() throws IOException {
		// Given
		addToRemoteBookmarksStore(new BookmarkId("folder1"));
		addToRemoteBookmarksStore(new BookmarkId("folder21"));
		remoteBookmarksStore.connect(new EmptyProgressIndicator());

		// When
		bookmarksTreeModifier.move(Lists.newArrayList(new BookmarkId("folder2")), new BookmarkId("folder11"));

		// Then
		Status status = validateModifications();
		assertThat(status.isOk()).isFalse();
	}

	private Status validateModifications() {
		for (BookmarksModification bookmarksModification : bookmarksTreeModifier.getModifications()) {
			Status status = bookmarksModificationValidator.validateModification(bookmarksModification);
			if (!status.isOk()) {
				return status;
			}
		}
		return Status.OK_STATUS;
	}

	private void addToRemoteBookmarksStore(BookmarkId bookmarkFolderId) throws IOException {
		remoteBookmarksStore.add(bookmarksTree.subTree(bookmarkFolderId), bookmarkFolderId, new EmptyProgressIndicator());
	}

	private BookmarksTree createBookmarksTree() {
		BookmarksTreeBuilder bookmarksTreeBuilder = bookmarksTree("root");
		bookmarksTreeBuilder.addBookmarks("root", bookmarkFolder("folder1"), bookmarkFolder("folder2"),
				bookmarkFolder(Constants.DEFAULT_BOOKMARKFOLDER_ID, "default"));
		bookmarksTreeBuilder.addBookmarks("folder1", bookmarkFolder("folder11"), bookmark("bookmark11"),
				bookmark("bookmark12"));
		bookmarksTreeBuilder.addBookmarks("folder2", bookmarkFolder("folder21"), bookmark("bookmark21"),
				bookmark("bookmark22"));

		return bookmarksTreeBuilder.build();
	}

}
