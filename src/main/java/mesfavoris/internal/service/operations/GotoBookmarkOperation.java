package mesfavoris.internal.service.operations;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import mesfavoris.BookmarksException;
import mesfavoris.IBookmarksMarkers;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.bookmarktype.IFileBookmarkLocation;
import mesfavoris.bookmarktype.IGotoBookmark;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;

import java.util.Optional;

public class GotoBookmarkOperation {
	private final Project project;
	private final BookmarkDatabase bookmarkDatabase;
	private final IBookmarkLocationProvider bookmarkLocationProvider;
	private final IGotoBookmark gotoBookmark;
	private final IBookmarksMarkers bookmarksMarkers;

	public GotoBookmarkOperation(Project project, BookmarkDatabase bookmarkDatabase, IBookmarkLocationProvider bookmarkLocationProvider,
								 IGotoBookmark gotoBookmark, IBookmarksMarkers bookmarksMarkers) {
		this.project = project;
		this.bookmarkDatabase = bookmarkDatabase;
		this.bookmarkLocationProvider = bookmarkLocationProvider;
		this.gotoBookmark = gotoBookmark;
		this.bookmarksMarkers = bookmarksMarkers;
	}

	public void gotoBookmark(BookmarkId bookmarkId, ProgressIndicator progress) throws BookmarksException {
		Bookmark bookmark = bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId);
		IBookmarkLocation bookmarkLocation = bookmarkLocationProvider.getBookmarkLocation(project, bookmark, progress);
		if (bookmarkLocation == null) {
			throw new BookmarksException("Could not find bookmark");
		}
		ApplicationManager.getApplication().invokeLater(() -> {
			if (gotoBookmark.gotoBookmark(project, bookmark, bookmarkLocation)) {
				if (bookmarkLocation instanceof IFileBookmarkLocation fileBookmarkLocation) {
					refreshMarker(bookmark, fileBookmarkLocation);
				}
			}
		});
	}

	private void refreshMarker(Bookmark bookmark, IFileBookmarkLocation fileBookmarkLocation) {
		AppExecutorUtil.getAppExecutorService().submit(() -> {
			bookmarksMarkers.refreshMarker(bookmark.getId(), Optional.of(fileBookmarkLocation));
		});
	}

}
