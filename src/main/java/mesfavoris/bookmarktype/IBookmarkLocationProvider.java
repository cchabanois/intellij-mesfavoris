package mesfavoris.bookmarktype;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.model.Bookmark;

public interface IBookmarkLocationProvider {

	/**
	 * Get the location corresponding to the given {@link Bookmark}
	 * 
	 * @param bookmark
	 * @return the bookmark location or null if not found
	 */
	IBookmarkLocation getBookmarkLocation(Project project, Bookmark bookmark, ProgressIndicator progress);

}
