package mesfavoris.internal.service.operations;

import mesfavoris.BookmarksException;
import mesfavoris.model.*;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class AddBookmarkFolderOperation {
	private final BookmarkDatabase bookmarkDatabase;

	public AddBookmarkFolderOperation(BookmarkDatabase bookmarkDatabase) {
		this.bookmarkDatabase = bookmarkDatabase;
	}

	public BookmarkId addBookmarkFolder(BookmarkId parentFolderId, String folderName) throws BookmarksException {
		BookmarkId id = new BookmarkId(UUID.randomUUID().toString());
		BookmarkFolder bookmarkFolder = new BookmarkFolder(id, folderName);
		addBookmarkFolder(parentFolderId, bookmarkFolder);
		return id;
	}

	private void addBookmarkFolder(final BookmarkId parentFolderId, final BookmarkFolder bookmarkFolder)
			throws BookmarksException {
		bookmarkDatabase.modify(bookmarksTreeModifier -> {
			Optional<BookmarkId> latestFolderBookmark = latestFolderBookmark(bookmarksTreeModifier.getCurrentTree(),
					parentFolderId);
			bookmarksTreeModifier.addBookmarksAfter(parentFolderId, latestFolderBookmark.orElse(null),
                    Collections.singletonList(bookmarkFolder));
		});
	}

	private Optional<BookmarkId> latestFolderBookmark(BookmarksTree bookmarksTree, BookmarkId parentFolderId) {
		return bookmarksTree.getChildren(parentFolderId).stream().filter(bookmark -> bookmark instanceof BookmarkFolder)
				.map(Bookmark::getId).reduce((a, b) -> b);
	}

}
