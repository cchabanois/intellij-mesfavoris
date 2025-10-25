package mesfavoris.internal.service.operations;

import com.intellij.openapi.ide.CopyPasteManager;
import mesfavoris.internal.model.copy.BookmarksCopier;
import mesfavoris.internal.model.utils.BookmarksTreeUtils;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.modification.BookmarksTreeModifier;
import mesfavoris.persistence.json.BookmarksTreeJsonSerializer;

import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * Copy a set of bookmarks to the clipboard
 *
 * @author cchabanois
 *
 */
public class CopyBookmarkOperation {

	public void copyToClipboard(BookmarksTree bookmarksTree, List<BookmarkId> selection) {
		if (selection.isEmpty()) {
			return;
		}
		String json = getSelectionAsJson(bookmarksTree, selection);
		if (json != null) {
			CopyPasteManager.getInstance().setContents(new StringSelection(json));
		}
	}

	public boolean hasDuplicatedBookmarksInSelection(BookmarksTree bookmarksTree, List<BookmarkId> selection) {
		return selection.size() != BookmarksTreeUtils.getUnduplicatedBookmarks(bookmarksTree, selection).size();
	}

	private String getSelectionAsJson(BookmarksTree bookmarksTree, List<BookmarkId> selection) {
		BookmarksTree selectionAsBookmarksTree = getSelectionAsBookmarksTree(bookmarksTree, selection);
		BookmarksTreeJsonSerializer serializer = new BookmarksTreeJsonSerializer(true);
		StringWriter writer = new StringWriter();
		try {
			serializer.serialize(selectionAsBookmarksTree, selectionAsBookmarksTree.getRootFolder().getId(), writer);
			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}

	private BookmarksTree getSelectionAsBookmarksTree(BookmarksTree bookmarksTree, List<BookmarkId> selection) {
		BookmarkFolder bookmarkFolder = new BookmarkFolder(new BookmarkId("selection"), "selection");
		BookmarksTreeModifier bookmarksTreeModifier = new BookmarksTreeModifier(new BookmarksTree(bookmarkFolder));
		BookmarksCopier bookmarksCopier = new BookmarksCopier(bookmarksTree, id -> id);
		bookmarksCopier.copy(bookmarksTreeModifier, bookmarkFolder.getId(),
				BookmarksTreeUtils.getUnduplicatedBookmarks(bookmarksTree, selection));
		return bookmarksTreeModifier.getCurrentTree();
	}

}
