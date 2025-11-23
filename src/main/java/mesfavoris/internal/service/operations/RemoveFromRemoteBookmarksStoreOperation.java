package mesfavoris.internal.service.operations;

import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.BookmarksException;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarksStoreManager;

import java.io.IOException;

public class RemoveFromRemoteBookmarksStoreOperation {
	private final BookmarkDatabase bookmarkDatabase;
	private final RemoteBookmarksStoreManager remoteBookmarksStoreManager;

	public RemoveFromRemoteBookmarksStoreOperation(BookmarkDatabase bookmarkDatabase,
			RemoteBookmarksStoreManager remoteBookmarksStoreManager) {
		this.bookmarkDatabase = bookmarkDatabase;
		this.remoteBookmarksStoreManager = remoteBookmarksStoreManager;
	}

	public void removeFromRemoteBookmarksStore(String storeId, final BookmarkId bookmarkFolderId,
			final ProgressIndicator monitor) throws BookmarksException {
		final IRemoteBookmarksStore store = remoteBookmarksStoreManager.getRemoteBookmarksStore(storeId)
				.orElseThrow(() -> new BookmarksException("Unknown store id"));
		bookmarkDatabase.modify(bookmarksTreeModifier -> {
			try {
				store.remove(bookmarkFolderId, monitor);
			} catch (IOException e) {
				throw new BookmarksException("Could not remove bookmark folder from store", e);
			}
		});
	}

	public boolean canRemoveFromRemoteBookmarkStore(String storeId, BookmarkId bookmarkFolderId) {
		IRemoteBookmarksStore store = remoteBookmarksStoreManager.getRemoteBookmarksStore(storeId).orElse(null);
		if (store == null) {
			return false;
		}
		return canRemoveFromRemoteBookmarkStore(store, bookmarkFolderId);
	}

	private boolean canRemoveFromRemoteBookmarkStore(IRemoteBookmarksStore remoteBookmarksStore,
			BookmarkId bookmarkFolderId) {
		if (remoteBookmarksStore.getState() != IRemoteBookmarksStore.State.connected) {
			return false;
		}
		return remoteBookmarksStore.getRemoteBookmarkFolder(bookmarkFolderId).isPresent();
	}

}
