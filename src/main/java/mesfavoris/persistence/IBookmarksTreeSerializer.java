package mesfavoris.persistence;

import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;

import java.io.IOException;
import java.io.Writer;

/**
 * Serialize a {@link BookmarksTree}
 * 
 * @author cchabanois
 *
 */
public interface IBookmarksTreeSerializer {

	/**
	 * Serialize the given bookmarks subTree
	 * 
	 * @param bookmarksTree
	 * @param bookmarkFolderId
	 *            the subtree to serialize
	 * @param writer
	 * @throws IOException
	 */
	public void serialize(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId, Writer writer)
			throws IOException;

}