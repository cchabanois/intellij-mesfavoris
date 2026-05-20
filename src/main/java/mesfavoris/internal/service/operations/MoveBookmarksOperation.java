package mesfavoris.internal.service.operations;

import mesfavoris.BookmarksException;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.service.MoveLocation;

import java.util.List;

public class MoveBookmarksOperation {
    private final BookmarkDatabase bookmarkDatabase;

    public MoveBookmarksOperation(BookmarkDatabase bookmarkDatabase) {
        this.bookmarkDatabase = bookmarkDatabase;
    }

    public void moveBookmarks(List<BookmarkId> ids, BookmarkId targetId, MoveLocation location) throws BookmarksException {
        bookmarkDatabase.modify(modifier -> {
            switch (location) {
                case INTO:
                    modifier.move(ids, targetId);
                    break;
                case BEFORE: {
                    BookmarkFolder parent = modifier.getCurrentTree().getParentBookmark(targetId);
                    if (parent == null) throw new BookmarksException("Target bookmark has no parent");
                    modifier.moveBefore(ids, parent.getId(), targetId);
                    break;
                }
                case AFTER: {
                    BookmarkFolder parent = modifier.getCurrentTree().getParentBookmark(targetId);
                    if (parent == null) throw new BookmarksException("Target bookmark has no parent");
                    modifier.moveAfter(ids, parent.getId(), targetId);
                    break;
                }
            }
        });
    }
}
