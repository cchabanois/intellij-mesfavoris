package mesfavoris.internal.validation;

import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.modification.BookmarksModification;
import mesfavoris.model.modification.IBookmarksModificationValidator;

public class AcceptAllBookmarksModificationValidator implements IBookmarksModificationValidator {

	@Override
	public void validateModification(BookmarksModification bookmarksModification) {

	}

	@Override
	public void validateModification(BookmarksTree bookmarksTree, BookmarkId bookmarkId) {

	}

}
