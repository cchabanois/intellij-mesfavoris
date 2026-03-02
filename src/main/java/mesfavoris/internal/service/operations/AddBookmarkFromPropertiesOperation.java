package mesfavoris.internal.service.operations;

import mesfavoris.BookmarksException;
import mesfavoris.internal.service.operations.utils.INewBookmarkPositionProvider;
import mesfavoris.internal.service.operations.utils.NewBookmarkPosition;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AddBookmarkFromPropertiesOperation {
    private final BookmarkDatabase bookmarkDatabase;
    private final INewBookmarkPositionProvider newBookmarkPositionProvider;

    public AddBookmarkFromPropertiesOperation(BookmarkDatabase bookmarkDatabase, INewBookmarkPositionProvider newBookmarkPositionProvider) {
        this.bookmarkDatabase = bookmarkDatabase;
        this.newBookmarkPositionProvider = newBookmarkPositionProvider;
    }

    public BookmarkId addBookmark(Map<String, String> properties) throws BookmarksException {
        Map<String, String> bookmarkProperties = new HashMap<>(properties);
        bookmarkProperties.putIfAbsent(Bookmark.PROPERTY_CREATED, Instant.now().toString());

        BookmarkId bookmarkId = new BookmarkId();
        Bookmark bookmark = new Bookmark(bookmarkId, properties);
        addBookmarkToTree(bookmark);
        return bookmarkId;
    }

    private void addBookmarkToTree(final Bookmark bookmark) throws BookmarksException {
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
