package mesfavoris.url.internal;

import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IGotoBookmark;
import mesfavoris.model.Bookmark;

import java.net.URISyntaxException;

public class GotoUrlBookmark implements IGotoBookmark {

	@Override
	public boolean gotoBookmark(Project project, Bookmark bookmark, IBookmarkLocation bookmarkLocation) {
		if (!(bookmarkLocation instanceof UrlBookmarkLocation urlBookmarkLocation)) {
			return false;
		}
        try {
			BrowserLauncher.getInstance().browse(urlBookmarkLocation.getUrl().toURI());
			return true;
		} catch (URISyntaxException e) {
			return false;
		}
	}

}
