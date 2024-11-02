package mesfavoris.url.internal;

import mesfavoris.bookmarktype.AbstractBookmarkLabelProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.url.BookmarkIcon;
import mesfavoris.url.UrlIcons;

import javax.swing.*;
import java.util.Base64;

import static mesfavoris.url.UrlBookmarkProperties.*;

public class UrlBookmarkLabelProvider extends AbstractBookmarkLabelProvider {

	@Override
	public Icon getIcon(Context context, Bookmark bookmark) {
		String iconAsBase64 = bookmark.getPropertyValue(PROP_ICON);
		if (iconAsBase64 == null) {
			iconAsBase64 = bookmark.getPropertyValue(PROP_FAVICON);
		}
		if (iconAsBase64 == null) {
			if (bookmark.getPropertyValue(PROP_URL) != null) {
				// taken from http://www.famfamfam.com/lab/icons/mini/icons/page_tag_blue.gif
				return UrlIcons.pageTagBlue;
			} else {
				return null;
			}
		}
		byte[] favIconBytes = Base64.getDecoder().decode(iconAsBase64);
		return new BookmarkIcon(favIconBytes);
	}
	
	@Override
	public boolean canHandle(Context context, Bookmark bookmark) {
		return bookmark.getPropertyValue(PROP_URL) != null;
	}

}