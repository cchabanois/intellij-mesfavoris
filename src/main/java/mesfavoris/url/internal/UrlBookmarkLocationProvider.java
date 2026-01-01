package mesfavoris.url.internal;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.model.Bookmark;

import java.net.URI;

import static mesfavoris.url.UrlBookmarkProperties.PROP_URL;

public class UrlBookmarkLocationProvider implements IBookmarkLocationProvider {

	@Override
	public IBookmarkLocation getBookmarkLocation(Project project, Bookmark bookmark, ProgressIndicator progress) {
		String url = bookmark.getPropertyValue(PROP_URL);
		if (url == null) {
			return null;
		}
		try {
			return new UrlBookmarkLocation(new URI(url).toURL());
		} catch (Exception e) {
			return null;
		}
	}

}
