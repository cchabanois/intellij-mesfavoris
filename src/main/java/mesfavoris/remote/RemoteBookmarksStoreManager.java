package mesfavoris.remote;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Manager for remote bookmarks stores.
 * This is a project-level service that provides access to all remote bookmarks stores.
 */
@Service(Service.Level.PROJECT)
public final class RemoteBookmarksStoreManager {
	private final Supplier<List<IRemoteBookmarksStore>> remoteBookmarksStoreProvider;

	/**
	 * Gets stores from the RemoteBookmarksStoreExtensionManager.
	 *
	 * @param project The project for which to manage remote bookmarks stores
	 */
	public RemoteBookmarksStoreManager(@NotNull Project project) {
		RemoteBookmarksStoreExtensionManager extensionManager = project.getService(RemoteBookmarksStoreExtensionManager.class);
		this.remoteBookmarksStoreProvider = extensionManager::getStores;
	}

	/**
	 * Constructor for testing.
	 * Allows providing a custom supplier of stores.
	 *
	 * @param remoteBookmarksStoreProvider Supplier that provides the list of remote bookmarks stores
	 */
	public RemoteBookmarksStoreManager(@NotNull Supplier<List<IRemoteBookmarksStore>> remoteBookmarksStoreProvider) {
		this.remoteBookmarksStoreProvider = remoteBookmarksStoreProvider;
	}

	public Collection<IRemoteBookmarksStore> getRemoteBookmarksStores() {
		return remoteBookmarksStoreProvider.get();
	}

	public Optional<IRemoteBookmarksStore> getRemoteBookmarksStore(String id) {
		return remoteBookmarksStoreProvider.get().stream()
				.filter(store -> id.equals(store.getDescriptor().id()))
				.findAny();
	}

	public Optional<RemoteBookmarkFolder> getRemoteBookmarkFolderContaining(BookmarksTree bookmarksTree, BookmarkId bookmarkId) {
		Bookmark bookmark = bookmarksTree.getBookmark(bookmarkId);
		if (bookmark == null) {
			return Optional.empty();
		}
		BookmarkFolder bookmarkFolder;
		if (bookmark instanceof BookmarkFolder) {
			bookmarkFolder = (BookmarkFolder) bookmark;
		} else {
			bookmarkFolder = bookmarksTree.getParentBookmark(bookmark.getId());
		}
		return getRemoteBookmarkFolderContaining(bookmarksTree, bookmarkFolder);
	}

	private Optional<RemoteBookmarkFolder> getRemoteBookmarkFolderContaining(BookmarksTree bookmarksTree,
			BookmarkFolder bookmarkFolder) {
		Optional<RemoteBookmarkFolder> remoteBookmarkFolder = getRemoteBookmarkFolder(bookmarkFolder.getId());
		if (remoteBookmarkFolder.isPresent()) {
			return remoteBookmarkFolder;
		}
		BookmarkFolder parent = bookmarksTree.getParentBookmark(bookmarkFolder.getId());
		if (parent == null) {
			return Optional.empty();
		} else {
			return getRemoteBookmarkFolderContaining(bookmarksTree, parent);
		}
	}

	public Optional<RemoteBookmarkFolder> getRemoteBookmarkFolder(BookmarkId bookmarkFolderId) {
		for (IRemoteBookmarksStore store : getRemoteBookmarksStores()) {
			Optional<RemoteBookmarkFolder> remoteBookmarkFolder = store.getRemoteBookmarkFolder(bookmarkFolderId);
			if (remoteBookmarkFolder.isPresent()) {
				return remoteBookmarkFolder;
			}
		}
		return Optional.empty();
	}

}
