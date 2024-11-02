package mesfavoris.bookmarktype;

import mesfavoris.icons.MesFavorisIcons;
import mesfavoris.model.Bookmark;
import mesfavoris.ui.renderers.StyledString;

import javax.swing.*;

public abstract class AbstractBookmarkLabelProvider implements IBookmarkLabelProvider {
	
	@Override
	public StyledString getStyledText(Context context, Bookmark bookmark) {
		String name = bookmark.getPropertyValue(Bookmark.PROPERTY_NAME);
		if (name == null) {
			name = "unnamed";
		}
		return new StyledString(name);
	}
	
	@Override
	public Icon getIcon(Context context, Bookmark bookmark) {
		return MesFavorisIcons.bookmark;
	}
	
}
