package mesfavoris.ui.renderers;

import com.intellij.icons.AllIcons;
import mesfavoris.bookmarktype.AbstractBookmarkLabelProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;

import javax.swing.*;

public class BookmarkFolderLabelProvider extends AbstractBookmarkLabelProvider {

	public BookmarkFolderLabelProvider() {
	}

	@Override
	public Icon getIcon(Context context, Bookmark bookmark) {
		return AllIcons.Nodes.Folder;
	}

	@Override
	public boolean canHandle(Context context, Bookmark bookmark) {
		return bookmark instanceof BookmarkFolder;
	}

}
