package mesfavoris.internal.service.operations;

import com.google.common.collect.Lists;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.BookmarksException;
import mesfavoris.internal.remote.InMemoryRemoteBookmarksStore;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ImportRemoteBookmarksTreeOperationTest extends BasePlatformTestCase {
	private ImportRemoteBookmarksTreeOperation operation;
	private BookmarkDatabase bookmarkDatabase;
	private InMemoryRemoteBookmarksStore remoteBookmarksStore;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.bookmarkDatabase = new BookmarkDatabase("test", createBookmarksTree());
		this.remoteBookmarksStore = new InMemoryRemoteBookmarksStore(getProject());
		this.remoteBookmarksStore.connect(new EmptyProgressIndicator());
		RemoteBookmarksStoreManager remoteBookmarksStoreManager = new RemoteBookmarksStoreManager(
				() -> Lists.newArrayList(remoteBookmarksStore));
		this.operation = new ImportRemoteBookmarksTreeOperation(bookmarkDatabase, remoteBookmarksStoreManager);
	}

	@Test
	public void testImportUnderNonRemoteFolder() throws Exception {
		// Given
		AtomicBoolean afterCommitRan = new AtomicBoolean(false);

		// When
		operation.importRemoteBookmarksTree(new BookmarkId("folder2"), createImportedTree(),
				tree -> afterCommitRan.set(true));

		// Then
		BookmarksTree tree = bookmarkDatabase.getBookmarksTree();
		assertThat(tree.getBookmark(new BookmarkId("imported"))).isNotNull();
		assertThat(tree.getParentBookmark(new BookmarkId("imported")).getId()).isEqualTo(new BookmarkId("folder2"));
		assertThat(afterCommitRan).isTrue();
	}

	@Test
	public void testCannotImportUnderRemoteBookmarkFolder() throws Exception {
		// Given
		makeRemote(new BookmarkId("folder1"));
		AtomicBoolean afterCommitRan = new AtomicBoolean(false);

		// When / Then
		assertThatThrownBy(() -> operation.importRemoteBookmarksTree(new BookmarkId("folder1"), createImportedTree(),
				tree -> afterCommitRan.set(true)))
				.isInstanceOf(BookmarksException.class);
		assertThat(afterCommitRan).isFalse();
		assertThat(bookmarkDatabase.getBookmarksTree().getBookmark(new BookmarkId("imported"))).isNull();
	}

	@Test
	public void testCannotImportUnderDescendantOfRemoteBookmarkFolder() throws Exception {
		// Given: folder11 is a descendant of the remote folder1
		makeRemote(new BookmarkId("folder1"));

		// When / Then
		assertThatThrownBy(() -> operation.importRemoteBookmarksTree(new BookmarkId("folder11"), createImportedTree(),
				tree -> {
				}))
				.isInstanceOf(BookmarksException.class);
	}

	private void makeRemote(BookmarkId bookmarkFolderId) throws Exception {
		remoteBookmarksStore.add(bookmarkDatabase.getBookmarksTree(), bookmarkFolderId, new EmptyProgressIndicator());
	}

	private BookmarksTree createBookmarksTree() {
		BookmarksTreeBuilder builder = bookmarksTree("root");
		builder.addBookmarks("root", bookmarkFolder("folder1"), bookmarkFolder("folder2"));
		builder.addBookmarks("folder1", bookmarkFolder("folder11"), bookmark("bookmark11"));
		return builder.build();
	}

	private BookmarksTree createImportedTree() {
		BookmarksTreeBuilder builder = bookmarksTree("imported");
		builder.addBookmarks("imported", bookmark("importedBookmark1"), bookmark("importedBookmark2"));
		return builder.build();
	}

}
