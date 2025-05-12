package mesfavoris.internal.service.operations;

import mesfavoris.BookmarksException;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;

import java.util.Map;

public class SetBookmarkPropertiesOperation {
    private final BookmarkDatabase bookmarkDatabase;

    public SetBookmarkPropertiesOperation(BookmarkDatabase bookmarkDatabase) {
        this.bookmarkDatabase = bookmarkDatabase;
    }

    public void setProperties(BookmarkId bookmarkId, Map<String,String> properties) throws BookmarksException {
        bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier.setProperties(bookmarkId, properties));
    }
}
