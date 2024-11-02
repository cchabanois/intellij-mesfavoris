package mesfavoris;

import com.intellij.openapi.vfs.VirtualFile;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.model.BookmarkId;

import java.util.List;

public interface IBookmarksMarkers {

	BookmarkMarker getMarker(BookmarkId bookmarkId);

	List<BookmarkMarker> getMarkers(VirtualFile file);

	void refreshMarker(BookmarkId bookmarkId);
	
	void deleteMarker(BookmarkId bookmarkId);

}