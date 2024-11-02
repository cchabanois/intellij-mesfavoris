package mesfavoris.bookmarktype;

import com.intellij.openapi.project.Project;
import mesfavoris.model.Bookmark;

public interface IGotoBookmark {

	/**
	 * Go to the location corresponding to the given {@link Bookmark}
	 * 
	 * @param bookmark
	 * @param bookmarkLocation
	 * @return true if it succeeds, false otherwise
	 */
	public boolean gotoBookmark(Project project, Bookmark bookmark, IBookmarkLocation bookmarkLocation);

}
