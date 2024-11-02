package mesfavoris.internal.bookmarktypes;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.bookmarktype.IBookmarkMarkerAttributesProvider;
import mesfavoris.model.Bookmark;

import java.util.ArrayList;
import java.util.List;

public class BookmarkMarkerAttributesProvider implements
		IBookmarkMarkerAttributesProvider {
	private final List<IBookmarkMarkerAttributesProvider> bookmarkMarkerAttributesProviders;

	public BookmarkMarkerAttributesProvider(
			List<IBookmarkMarkerAttributesProvider> bookmarkMarkerAttributesProviders) {
		this.bookmarkMarkerAttributesProviders = new ArrayList<IBookmarkMarkerAttributesProvider>(
				bookmarkMarkerAttributesProviders);
	}

	@Override
	public BookmarkMarker getMarkerDescriptor(Project project, Bookmark bookmark, ProgressIndicator progressIndicator) {
		for (IBookmarkMarkerAttributesProvider provider : bookmarkMarkerAttributesProviders) {
			BookmarkMarker bookmarkMarkerDescriptor = provider
					.getMarkerDescriptor(project, bookmark, progressIndicator /* TODO : fix */);
			if (bookmarkMarkerDescriptor != null) {
				return bookmarkMarkerDescriptor;
			}
		}
		return null;
	}
}
