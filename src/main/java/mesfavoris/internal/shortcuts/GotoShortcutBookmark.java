package mesfavoris.internal.shortcuts;

import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IGotoBookmark;
import mesfavoris.model.Bookmark;
import mesfavoris.service.IBookmarksService;

public class GotoShortcutBookmark implements IGotoBookmark {

	@Override
	public boolean gotoBookmark(Project project, Bookmark bookmark, IBookmarkLocation bookmarkLocation) {
		if (!(bookmarkLocation instanceof ShortcutBookmarkLocation shortcutBookmarkLocation)) {
			return false;
		}
        IBookmarksService bookmarksService = project.getService(IBookmarksService.class);
		if (bookmarksService.getBookmarksTree().getBookmark(shortcutBookmarkLocation.getBookmarkId()) == null) {
			return false;
		}
		bookmarksService.selectBookmarkInTree(shortcutBookmarkLocation.getBookmarkId());
		return true;
	}

}
