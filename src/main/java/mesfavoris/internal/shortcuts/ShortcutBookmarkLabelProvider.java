package mesfavoris.internal.shortcuts;

import com.intellij.openapi.project.Project;
import com.intellij.ui.LayeredIcon;
import mesfavoris.bookmarktype.AbstractBookmarkLabelProvider;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ShortcutBookmarkLabelProvider extends AbstractBookmarkLabelProvider {

	@Override
	public Icon getIcon(Project project, @NotNull Bookmark shortcutBookmark) {
		IBookmarksService bookmarksService = project.getService(IBookmarksService.class);
		IBookmarkLabelProvider bookmarkLabelProvider = bookmarksService.getBookmarkLabelProvider();

		BookmarkId bookmarkId = new BookmarkId(
				shortcutBookmark.getPropertyValue(ShortcutBookmarkProperties.PROP_BOOKMARK_ID));

		if (bookmarkId.equals(shortcutBookmark.getId())) {
			return null;
		}

		Bookmark bookmark = bookmarksService.getBookmarksTree().getBookmark(bookmarkId);

		Icon icon;
		if (bookmark != null) {
			icon = bookmarkLabelProvider.getIcon(project, bookmark);
		} else {
			icon = super.getIcon(project, shortcutBookmark);
		}

		Icon overlayIcon = ShortcutBookmarkIcons.linkOverlay;

		LayeredIcon layeredIcon = new LayeredIcon(2);
		layeredIcon.setIcon(icon, 0);
		int x = 0;
		int y = icon.getIconHeight() - overlayIcon.getIconHeight();
		layeredIcon.setIcon(overlayIcon, 1, x, y);

		return layeredIcon;
	}

	@Override
	public boolean canHandle(Project project, @NotNull Bookmark bookmark) {
		IBookmarksService bookmarksService = project.getService(IBookmarksService.class);
		return bookmarksService.getBookmarksTree() != null
				&& bookmark.getPropertyValue(ShortcutBookmarkProperties.PROP_BOOKMARK_ID) != null;
	}
}
