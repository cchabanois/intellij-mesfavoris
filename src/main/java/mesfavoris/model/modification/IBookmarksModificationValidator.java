package mesfavoris.model.modification;

import mesfavoris.BookmarksException;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;

public interface IBookmarksModificationValidator {

	/**
	 * Validate that the given modification is allowed
	 * 
	 * @param bookmarksModification
	 * @return
	 */
	void validateModification(BookmarksModification bookmarksModification) throws BookmarksException;

	/**
	 * Check if given bookmark can be modified
	 * 
	 * @param bookmarksTree
	 * @param bookmarkId
	 * @return
	 */
	void validateModification(BookmarksTree bookmarksTree, BookmarkId bookmarkId) throws BookmarksException;

}