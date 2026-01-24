package mesfavoris.bookmarktype;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.model.Bookmark;

import java.util.Optional;

public interface IBookmarkMarkerAttributesProvider {

	/**
	 * Get descriptor to create a {@link BookmarkMarker} from a {@link Bookmark}
	 *
	 * @param project
	 * @param bookmark
	 * @param progressIndicator
	 * @return
	 */
	public BookmarkMarker getMarkerDescriptor(Project project, Bookmark bookmark, Optional<IFileBookmarkLocation> fileBookmarkLocation, ProgressIndicator progressIndicator);

}
