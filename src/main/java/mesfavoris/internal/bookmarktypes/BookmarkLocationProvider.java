package mesfavoris.internal.bookmarktypes;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.model.Bookmark;

import java.util.ArrayList;
import java.util.List;

public class BookmarkLocationProvider implements IBookmarkLocationProvider {
	private final List<IBookmarkLocationProvider> bookmarkLocationProviders;

	public BookmarkLocationProvider(List<IBookmarkLocationProvider> bookmarkLocationProviders) {
		this.bookmarkLocationProviders = new ArrayList<IBookmarkLocationProvider>(bookmarkLocationProviders);
	}

	@Override
	public IBookmarkLocation getBookmarkLocation(Project project, Bookmark bookmark, ProgressIndicator progress) {
		float bestScore = 0.0f;
		IBookmarkLocation bestBookmarkLocation = null;
		for (IBookmarkLocationProvider provider : bookmarkLocationProviders) {
			IBookmarkLocation bookmarkLocation = /* ReadAction.compute(() ->  */ provider.getBookmarkLocation(project, bookmark, progress); // );
			if (bookmarkLocation != null && bookmarkLocation.getScore() > bestScore) {
				bestBookmarkLocation = bookmarkLocation;
				bestScore = bookmarkLocation.getScore();
				if (bestScore >= IBookmarkLocation.MAX_SCORE) {
					break;
				}
			}
		}
		return bestBookmarkLocation;
	}

}
