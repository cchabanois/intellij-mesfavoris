package mesfavoris.internal.markers;

import com.intellij.openapi.vfs.VirtualFile;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.model.BookmarkId;

import java.util.List;

public interface IBookmarksMarkersStore {
    BookmarkMarker put(BookmarkMarker bookmarkMarker);

    BookmarkMarker remove(BookmarkId bookmarkId);

    BookmarkMarker get(BookmarkId bookmarkId);

    List<BookmarkMarker> get(VirtualFile file);
}
