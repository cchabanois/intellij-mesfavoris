package mesfavoris.internal.bookmarktypes;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IGotoBookmark;
import mesfavoris.model.Bookmark;

import java.util.ArrayList;
import java.util.List;

public class GotoBookmark implements IGotoBookmark {
	private static final Logger LOG = Logger.getInstance(GotoBookmark.class);

	private final List<IGotoBookmark> gotoBookmarks;

	public GotoBookmark(List<IGotoBookmark> gotoBookmarks) {
		this.gotoBookmarks = new ArrayList<IGotoBookmark>(gotoBookmarks);
	}

	@Override
	public boolean gotoBookmark(Project project, Bookmark bookmark, IBookmarkLocation bookmarkLocation) {
		for (IGotoBookmark gotoBookmark : gotoBookmarks) {
			if (gotoBookmark(project, gotoBookmark, bookmark, bookmarkLocation)) {
				return true;
			}
		}
		return false;
	}

	private boolean gotoBookmark(Project project, IGotoBookmark gotoBookmark, Bookmark bookmark,
			IBookmarkLocation bookmarkLocation) {
		try {
			return gotoBookmark.gotoBookmark(project, bookmark, bookmarkLocation);
		} catch(Exception|LinkageError|AssertionError e) {
			LOG.error(e);
			return false;
		}
	}

}
