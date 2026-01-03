package mesfavoris.internal.service.operations;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.BookmarksException;
import mesfavoris.commons.SubProgressIndicator;
import mesfavoris.internal.model.merge.BookmarksTreeMerger;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.LockMode;
import mesfavoris.model.OptimisticLockException;
import mesfavoris.persistence.IBookmarksDirtyStateTracker;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.IRemoteBookmarksStore.State;
import mesfavoris.remote.RemoteBookmarkFolder;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.remote.RemoteBookmarksTree;

import java.io.IOException;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Refresh remote folders in the bookmark database. Load remote bookmarks from
 * remote stores and replace them in the bookmark database.
 * 
 * @author cchabanois
 *
 */
public class RefreshRemoteFolderOperation {
	private final BookmarkDatabase bookmarkDatabase;
	private final RemoteBookmarksStoreManager remoteBookmarksStoreManager;
	private final IBookmarksDirtyStateTracker bookmarksDirtyStateTracker;

	public RefreshRemoteFolderOperation(BookmarkDatabase bookmarkDatabase,
			RemoteBookmarksStoreManager remoteBookmarksStoreManager,
			IBookmarksDirtyStateTracker bookmarksDirtyStateTracker) {
		this.bookmarkDatabase = bookmarkDatabase;
		this.remoteBookmarksStoreManager = remoteBookmarksStoreManager;
		this.bookmarksDirtyStateTracker = bookmarksDirtyStateTracker;
	}

	public void refresh(ProgressIndicator monitor) throws BookmarksException {
		Collection<IRemoteBookmarksStore> stores = remoteBookmarksStoreManager.getRemoteBookmarksStores();
		List<String> storeIds = stores.stream().filter(store -> store.getState() == State.connected)
				.map(store -> store.getDescriptor().id()).toList();
		monitor.setText("Loading bookmark folders from all stores");
		monitor.setIndeterminate(false);
		monitor.setFraction(0.0);

		Exception exception = null;
		double progressPerStore = storeIds.isEmpty() ? 0.0 : 1.0 / storeIds.size();
		int storeIndex = 0;

		for (String storeId : storeIds) {
			try {
				double baseFraction = storeIndex * progressPerStore;
				monitor.setFraction(baseFraction);
				try (SubProgressIndicator subMonitor = new SubProgressIndicator(monitor, progressPerStore)) {
					refresh(storeId, subMonitor);
				}
			} catch (ProcessCanceledException e) {
				throw e;
			} catch (Exception e) {
				exception = e;
			}
			storeIndex++;
		}
		monitor.setFraction(1.0);

		if (exception != null) {
			if (exception instanceof BookmarksException) {
				throw (BookmarksException) exception;
			} else {
				throw (RuntimeException) exception;
			}
		}
	}

	public void refresh(String storeId, ProgressIndicator monitor) throws BookmarksException {
		IRemoteBookmarksStore store = remoteBookmarksStoreManager.getRemoteBookmarksStore(storeId)
				.orElseThrow(() -> new BookmarksException("Remote bookmarks store not found"));
		Set<RemoteBookmarkFolder> remoteBookmarkFolders = store.getRemoteBookmarkFolders();
		monitor.setText("Loading bookmark folders from " + store.getDescriptor().label());
		monitor.setIndeterminate(false);
		monitor.setFraction(0.0);

		Exception exception = null;
		double progressPerFolder = remoteBookmarkFolders.isEmpty() ? 0.0 : 1.0 / remoteBookmarkFolders.size();
		int folderIndex = 0;

		for (RemoteBookmarkFolder bookmarkFolder : remoteBookmarkFolders) {
			try {
				double baseFraction = folderIndex * progressPerFolder;
				monitor.setFraction(baseFraction);
				try (SubProgressIndicator subMonitor = new SubProgressIndicator(monitor, progressPerFolder)) {
					refresh(bookmarkFolder.getBookmarkFolderId(), subMonitor);
				}
			} catch (ProcessCanceledException e) {
				throw e;
			} catch (Exception e) {
				exception = e;
			}
			folderIndex++;
		}
		monitor.setFraction(1.0);

		if (exception != null) {
			if (exception instanceof BookmarksException) {
				throw (BookmarksException) exception;
			} else {
				throw (RuntimeException) exception;
			}
		}
	}

	public void refresh(BookmarkId bookmarkFolderId, ProgressIndicator monitor) throws BookmarksException {
		IRemoteBookmarksStore store = remoteBookmarksStoreManager.getRemoteBookmarkFolder(bookmarkFolderId)
				.flatMap(remoteBookmarkFolder -> remoteBookmarksStoreManager.getRemoteBookmarksStore(remoteBookmarkFolder.getRemoteBookmarkStoreId()))
				.orElseThrow(() -> new BookmarksException("Not a remote bookmark folder"));
		do {
			try {
				bookmarkDatabase.modify(LockMode.OPTIMISTIC, (bookmarksTreeModifier) -> {
					if (bookmarksDirtyStateTracker.isDirty()) {
						throw new DirtyBookmarksDatabaseException();
					}
					try {
						RemoteBookmarksTree remoteBookmarksTree = store.load(bookmarkFolderId, monitor);
						// replace existing bookmark folder with remote one
						BookmarksTreeMerger bookmarksTreeMerger = new BookmarksTreeMerger(
								remoteBookmarksTree.getBookmarksTree());
						bookmarksTreeMerger.merge(bookmarksTreeModifier);
					} catch (IOException e) {
						throw new BookmarksException("Could not load remote bookmark folder", e);
					}
				}, /* validateModifications */ false);
				return;
			} catch (OptimisticLockException | DirtyBookmarksDatabaseException e) {
				try {
					// sleep and retry later
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					throw new BookmarksException("Could not load remote bookmark folder", e1);
				}
				if (monitor.isCanceled()) {
					throw new ProcessCanceledException();
				}
			}
		} while (true);
	}

	private static class DirtyBookmarksDatabaseException extends BookmarksException {
		@Serial
        private static final long serialVersionUID = 6024826805648888249L;

		public DirtyBookmarksDatabaseException() {
			super("Bookmark database is dirty");
		}

	}

}
