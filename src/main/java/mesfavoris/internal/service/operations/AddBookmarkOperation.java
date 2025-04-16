package mesfavoris.internal.service.operations;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.progress.ProgressIndicator;

import mesfavoris.BookmarksException;
import mesfavoris.bookmarktype.IBookmarkPropertiesProvider;
import mesfavoris.internal.service.operations.utils.INewBookmarkPositionProvider;
import mesfavoris.internal.service.operations.utils.NewBookmarkPosition;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;

public class AddBookmarkOperation {
	private final BookmarkDatabase bookmarkDatabase;
	private final INewBookmarkPositionProvider newBookmarkPositionProvider;
	private final IBookmarkPropertiesProvider bookmarkPropertiesProvider;

	public AddBookmarkOperation(BookmarkDatabase bookmarkDatabase,
			IBookmarkPropertiesProvider bookmarkPropertiesProvider,
			INewBookmarkPositionProvider newBookmarkPositionProvider) {
		this.bookmarkDatabase = bookmarkDatabase;
		this.newBookmarkPositionProvider = newBookmarkPositionProvider;
		this.bookmarkPropertiesProvider = bookmarkPropertiesProvider;
	}

	public BookmarkId addBookmark(DataContext dataContext, ProgressIndicator progress)
			throws BookmarksException {
		Map<String, String> bookmarkProperties = new HashMap<String, String>();
		bookmarkPropertiesProvider.addBookmarkProperties(bookmarkProperties, dataContext, progress);
		if (bookmarkProperties.isEmpty()) {
			throw new BookmarksException("Could not create bookmark from current selection");
		}
		bookmarkProperties.put(Bookmark.PROPERTY_CREATED, Instant.now().toString());
		BookmarkId bookmarkId = new BookmarkId();
		Bookmark bookmark = new Bookmark(bookmarkId, bookmarkProperties);
		addBookmark(bookmark);
		return bookmarkId;
	}

	private void addBookmark(final Bookmark bookmark) throws BookmarksException {
		NewBookmarkPosition newBookmarkPosition = newBookmarkPositionProvider.getNewBookmarkPosition();
		bookmarkDatabase.modify(bookmarksTreeModifier -> {
			if (newBookmarkPosition.getBookmarkId().isPresent()) {
				bookmarksTreeModifier.addBookmarksAfter(newBookmarkPosition.getParentBookmarkId(),
						newBookmarkPosition.getBookmarkId().get(), Collections.singletonList(bookmark));
			} else {
				bookmarksTreeModifier.addBookmarks(newBookmarkPosition.getParentBookmarkId(), Collections.singletonList(bookmark));
			}
		});
	}

}
