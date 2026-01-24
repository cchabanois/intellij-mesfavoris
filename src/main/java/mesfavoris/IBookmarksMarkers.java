package mesfavoris;

import com.intellij.openapi.vfs.VirtualFile;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.bookmarktype.IFileBookmarkLocation;
import mesfavoris.model.BookmarkId;

import java.util.List;
import java.util.Optional;

public interface IBookmarksMarkers {

	BookmarkMarker getMarker(BookmarkId bookmarkId);

	List<BookmarkMarker> getMarkers(VirtualFile file);

	void refreshMarker(BookmarkId bookmarkId, Optional<IFileBookmarkLocation> fileBookmarkLocation);

	void deleteMarker(BookmarkId bookmarkId);

}