package mesfavoris.internal.shortcuts;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;

public class ShortcutBookmarkLocationProvider implements IBookmarkLocationProvider {

	@Override
	public IBookmarkLocation getBookmarkLocation(Project project, Bookmark bookmark, ProgressIndicator progress) {
		String bookmarkIdAsString = bookmark.getPropertyValue(ShortcutBookmarkProperties.PROP_BOOKMARK_ID);
		if (bookmarkIdAsString == null) {
			return null;
		}
		return new ShortcutBookmarkLocation(new BookmarkId(bookmarkIdAsString));
	}

}
