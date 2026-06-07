package mesfavoris.persistence;

public interface IBookmarksDirtyStateTracker extends IDirtyBookmarksProvider {

	/**
	 * Check if the bookmarks database is dirty (with some changes not yet saved
	 * to local file and remote stores)
	 * 
	 * @return true if dirty
	 */
	default boolean isDirty() {
		return !getDirtyBookmarks().isEmpty();
	}

}