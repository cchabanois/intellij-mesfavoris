package mesfavoris.internal.bookmarktypes;

import mesfavoris.bookmarktype.AbstractBookmarkLabelProvider;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.icons.MesFavorisIcons;
import mesfavoris.model.Bookmark;
import mesfavoris.ui.renderers.StyledString;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class BookmarkLabelProvider implements IBookmarkLabelProvider {
	private final List<IBookmarkLabelProvider> bookmarkLabelProviders;

	public BookmarkLabelProvider() {
		this.bookmarkLabelProviders = new ArrayList<>();
		this.bookmarkLabelProviders.add(new DefaultBookmarkLabelProvider());
	}
	
	public BookmarkLabelProvider(List<IBookmarkLabelProvider> bookmarkLabelProviders) {
		this.bookmarkLabelProviders = new ArrayList<>();
		this.bookmarkLabelProviders.addAll(bookmarkLabelProviders);
		this.bookmarkLabelProviders.add(new DefaultBookmarkLabelProvider());
	}
	
	@Override
	public Icon getIcon(Context context, Bookmark bookmark) {
		Icon icon = getBookmarkLabelProvider(context, bookmark).getIcon(context, bookmark);
		if (icon == null) {
			icon = MesFavorisIcons.bookmark;
		}
		return icon;
	}
	
	@Override
	public StyledString getStyledText(Context context, Bookmark bookmark) {
		return getBookmarkLabelProvider(context, bookmark).getStyledText(context, bookmark);
	}

	private IBookmarkLabelProvider getBookmarkLabelProvider(Context context, Bookmark bookmark) {
		for (IBookmarkLabelProvider bookmarkLabelProvider : bookmarkLabelProviders) {
			if (bookmarkLabelProvider.canHandle(context, bookmark)) {
				return bookmarkLabelProvider;
			}
		}
		// will never happen
		return null;
	}
	
	@Override
	public boolean canHandle(Context context, Bookmark bookmark) {
		return true;
	}

	private static class DefaultBookmarkLabelProvider extends AbstractBookmarkLabelProvider {

		@Override
		public boolean canHandle(Context context, Bookmark bookmark) {
			return true;
		}
		
	}
	

}
