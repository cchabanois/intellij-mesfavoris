package mesfavoris.bookmarktype;

import mesfavoris.model.Bookmark;
import mesfavoris.ui.renderers.StyledString;

import javax.swing.*;

public interface IBookmarkLabelProvider {

	public StyledString getStyledText(Context context, Bookmark bookmark);

	public Icon getIcon(Context context, Bookmark bookmark);

	public boolean canHandle(Context context, Bookmark bookmark);

	public static interface Context {
		public static final String BOOKMARK_DATABASE_ID = "id";
		public static final String BOOKMARKS_TREE = "bookmarksTree";
		public static final String PROJECT = "project";
				
		public <T> T get(String name);
	}

}
