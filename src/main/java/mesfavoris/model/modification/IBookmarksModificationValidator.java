package mesfavoris.model.modification;

import mesfavoris.BookmarksException;
import mesfavoris.commons.Status;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;

public interface IBookmarksModificationValidator {

	/**
	 * Validate that the given modification is allowed
	 * 
	 * @param bookmarksModification
	 * @return
	 */
	Status validateModification(BookmarksModification bookmarksModification);

	/**
	 * Check if given bookmark can be modified
	 * 
	 * @param bookmarksTree
	 * @param bookmarkId
	 * @return
	 */
	Status validateModification(BookmarksTree bookmarksTree, BookmarkId bookmarkId);

}