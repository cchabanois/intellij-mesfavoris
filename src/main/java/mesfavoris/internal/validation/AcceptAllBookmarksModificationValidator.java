package mesfavoris.internal.validation;

import mesfavoris.commons.Status;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.modification.BookmarksModification;
import mesfavoris.model.modification.IBookmarksModificationValidator;

public class AcceptAllBookmarksModificationValidator implements IBookmarksModificationValidator {

	@Override
	public Status validateModification(BookmarksModification bookmarksModification) {
		return Status.OK_STATUS;
	}

	@Override
	public Status validateModification(BookmarksTree bookmarksTree, BookmarkId bookmarkId) {
		return Status.OK_STATUS;
	}

}
