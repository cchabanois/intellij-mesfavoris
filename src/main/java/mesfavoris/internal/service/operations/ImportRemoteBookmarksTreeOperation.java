package mesfavoris.internal.service.operations;

import java.util.function.Consumer;

import com.google.common.collect.Lists;

import mesfavoris.BookmarksException;
import mesfavoris.internal.model.copy.BookmarksCopier;
import mesfavoris.internal.model.copy.NonExistingBookmarkIdProvider;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.remote.RemoteBookmarksStoreManager;

/**
 * Graft a bookmarks tree imported from a remote store under a local folder and, once committed,
 * let the caller register the folder&rarr;remote mapping via {@code afterCommit}.
 * <p>
 * A remote bookmark folder cannot be nested under another remote bookmark folder: the mapping is
 * registered after the commit (out of reach of {@link mesfavoris.internal.validation.BookmarksModificationValidator}),
 * so this operation enforces the invariant up-front against the target parent.
 */
public class ImportRemoteBookmarksTreeOperation {
	private final BookmarkDatabase bookmarkDatabase;
	private final RemoteBookmarksStoreManager remoteBookmarksStoreManager;

	public ImportRemoteBookmarksTreeOperation(BookmarkDatabase bookmarkDatabase,
			RemoteBookmarksStoreManager remoteBookmarksStoreManager) {
		this.bookmarkDatabase = bookmarkDatabase;
		this.remoteBookmarksStoreManager = remoteBookmarksStoreManager;
	}

	public void importRemoteBookmarksTree(BookmarkId parentBookmarkId, BookmarksTree sourceBookmarksTree,
			Consumer<BookmarksTree> afterCommit) throws BookmarksException {
		if (remoteBookmarksStoreManager
				.getRemoteBookmarkFolderContaining(bookmarkDatabase.getBookmarksTree(), parentBookmarkId).isPresent()) {
			throw new BookmarksException(
					"Cannot import a remote bookmark folder under another remote bookmark folder");
		}
		bookmarkDatabase.modify(bookmarksTreeModifier -> {
			BookmarksCopier bookmarksCopier = new BookmarksCopier(sourceBookmarksTree,
					new NonExistingBookmarkIdProvider(bookmarksTreeModifier.getCurrentTree()));
			bookmarksCopier.copy(bookmarksTreeModifier, parentBookmarkId,
					Lists.newArrayList(sourceBookmarksTree.getRootFolder().getId()));
		}, afterCommit);
	}

}
