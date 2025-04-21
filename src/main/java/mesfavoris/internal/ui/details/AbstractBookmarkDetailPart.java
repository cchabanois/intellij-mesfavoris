package mesfavoris.internal.ui.details;

import java.util.List;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import mesfavoris.service.BookmarksService;

import mesfavoris.internal.model.compare.BookmarkComparer;
import mesfavoris.internal.model.compare.BookmarkComparer.BookmarkDifferences;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.IBookmarksListener;
import mesfavoris.model.modification.BookmarksModification;
import mesfavoris.ui.details.IBookmarkDetailPart;

public abstract class AbstractBookmarkDetailPart implements IBookmarkDetailPart {
	protected final BookmarkDatabase bookmarkDatabase;
	protected final Project project;
	protected Bookmark bookmark;
	private final IBookmarksListener bookmarksListener = this::handleModifications;

	public AbstractBookmarkDetailPart(Project project) {
		this.project = project;
		BookmarksService bookmarksService = project.getService(BookmarksService.class);
		this.bookmarkDatabase = bookmarksService.getBookmarkDatabase();
	}

	@Override
	public void init() {
		bookmarkDatabase.addListener(bookmarksListener);
	}

	@Override
	public void setBookmark(Bookmark bookmark) {
		this.bookmark = bookmark;
	}

	@Override
	public void dispose() {
		bookmarkDatabase.removeListener(bookmarksListener);
	}

	private void handleModifications(List<BookmarksModification> modifications) {
		if (!canHandle(bookmark) || bookmark == null) {
			return;
		}
		Bookmark newBookmark = bookmarkDatabase.getBookmarksTree().getBookmark(bookmark.getId());
		if (newBookmark != null) {
			BookmarkComparer bookmarkComparer = new BookmarkComparer();
			BookmarkDifferences bookmarkDifferences = bookmarkComparer.compare(bookmark, newBookmark);
			if (bookmarkDifferences.isEmpty()) {
				return;
			}
		}
		Bookmark oldBookmark = bookmark;
		bookmark = newBookmark;
		ApplicationManager.getApplication().invokeLater(() -> bookmarkModified(oldBookmark, bookmark));
	}

	/**
	 * Called when bookmark has been modified
	 * 
	 * @param oldBookmark
	 *            the bookmark before the modification
	 * @param newBookmark
	 *            the bookmark after the modification. Null if bookmark has been
	 *            deleted
	 */
	protected abstract void bookmarkModified(Bookmark oldBookmark, Bookmark newBookmark);

}
