package mesfavoris.internal.validation;

import mesfavoris.commons.Status;
import mesfavoris.internal.model.merge.BookmarksTreeIterable;
import mesfavoris.internal.model.merge.BookmarksTreeIterator.Algorithm;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.modification.*;
import mesfavoris.remote.IRemoteBookmarksStore.State;
import mesfavoris.remote.RemoteBookmarkFolder;
import mesfavoris.remote.RemoteBookmarksStoreManager;

import java.util.Optional;
import java.util.stream.StreamSupport;

public class BookmarksModificationValidator implements IBookmarksModificationValidator {
	private final RemoteBookmarksStoreManager remoteBookmarksStoreManager;

	public BookmarksModificationValidator(RemoteBookmarksStoreManager remoteBookmarksStoreManager) {
		this.remoteBookmarksStoreManager = remoteBookmarksStoreManager;
	}

	@Override
	public Status validateModification(BookmarksModification bookmarksModification) {
		if (bookmarksModification instanceof BookmarkDeletedModification) {
			return validate((BookmarkDeletedModification) bookmarksModification);
		}
		if (bookmarksModification instanceof BookmarksAddedModification) {
			return validate((BookmarksAddedModification) bookmarksModification);
		}
		if (bookmarksModification instanceof BookmarkPropertiesModification) {
			return validate((BookmarkPropertiesModification) bookmarksModification);
		}
		if (bookmarksModification instanceof BookmarksMovedModification) {
			return validate((BookmarksMovedModification) bookmarksModification);
		}
		return errorStatus("Unknown bookmarks modification");
	}

	private Status validate(BookmarkDeletedModification bookmarkDeletedModification) {
		return validateModification(bookmarkDeletedModification.getSourceTree(),
				bookmarkDeletedModification.getBookmarkParentId());
	}

	private Status validate(BookmarksAddedModification bookmarksAddedModification) {
		return validateModification(bookmarksAddedModification.getSourceTree(),
				bookmarksAddedModification.getParentId());
	}

	private Status validate(BookmarkPropertiesModification bookmarkPropertiesModification) {
		return validateModification(bookmarkPropertiesModification.getSourceTree(),
				bookmarkPropertiesModification.getBookmarkId());
	}

	private Status validate(BookmarksMovedModification bookmarksMovedModification) {
		Status status = validateModification(bookmarksMovedModification.getSourceTree(),
				bookmarksMovedModification.getNewParentId());
		if (!status.isOk()) {
			return status;
		}
		if (remoteBookmarksStoreManager.getRemoteBookmarkFolderContaining(bookmarksMovedModification.getSourceTree(),
				bookmarksMovedModification.getNewParentId()).isEmpty()) {
			return Status.OK_STATUS;
		}
		for (BookmarkId bookmarkId : bookmarksMovedModification.getBookmarkIds()) {
			if (containsRemoteBookmarkFolder(bookmarksMovedModification.getSourceTree(), bookmarkId)) {
				return errorStatus("Cannot move remote bookmark under another remote bookmark");
			}
		}
		return Status.OK_STATUS;
	}

	private boolean containsRemoteBookmarkFolder(BookmarksTree bookmarksTree, BookmarkId bookmarkId) {
		BookmarksTreeIterable bookmarksTreeIterable = new BookmarksTreeIterable(bookmarksTree, bookmarkId,
				Algorithm.PRE_ORDER);
		return StreamSupport.stream(bookmarksTreeIterable.spliterator(), false)
				.filter(bookmark -> remoteBookmarksStoreManager.getRemoteBookmarkFolder(bookmark.getId()).isPresent())
				.findFirst().isPresent();
	}

	public Status validateModification(BookmarksTree bookmarksTree, BookmarkId bookmarkId) {
		Bookmark bookmark = bookmarksTree.getBookmark(bookmarkId);
		if (bookmark == null) {
			return errorStatus("Cannot find bookmark");
		}
		Optional<RemoteBookmarkFolder> remoteBookmarkFolder = remoteBookmarksStoreManager
				.getRemoteBookmarkFolderContaining(bookmarksTree, bookmarkId);
		if (remoteBookmarkFolder.isEmpty()) {
			return Status.OK_STATUS;
		}
		boolean connected = remoteBookmarkFolder
				.flatMap(f -> remoteBookmarksStoreManager.getRemoteBookmarksStore(f.getRemoteBookmarkStoreId()))
				.map(remoteBookmarksStore -> remoteBookmarksStore.getState() == State.connected).orElse(false);

		if (connected && !isReadOnly(remoteBookmarkFolder.get())) {
			return Status.OK_STATUS;
		} else {
			return errorStatus(
					"Cannot modify bookmark that is under a shared folder that is not connected or readonly");
		}
	}

	private boolean isReadOnly(RemoteBookmarkFolder remoteBookmarkFolder) {
		return Boolean.TRUE.toString()
				.equalsIgnoreCase(remoteBookmarkFolder.getProperties().get(RemoteBookmarkFolder.PROP_READONLY));
	}

	private Status errorStatus(String message) {
		return new Status(Status.Severity.ERROR, 0, message, null);
	}

}
