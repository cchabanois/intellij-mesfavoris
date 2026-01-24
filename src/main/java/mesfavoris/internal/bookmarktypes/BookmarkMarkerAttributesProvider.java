package mesfavoris.internal.bookmarktypes;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.bookmarktype.IBookmarkMarkerAttributesProvider;
import mesfavoris.bookmarktype.IFileBookmarkLocation;
import mesfavoris.commons.SubProgressIndicator;
import mesfavoris.model.Bookmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookmarkMarkerAttributesProvider implements
		IBookmarkMarkerAttributesProvider {
	private final List<IBookmarkMarkerAttributesProvider> bookmarkMarkerAttributesProviders;

	public BookmarkMarkerAttributesProvider(
			List<IBookmarkMarkerAttributesProvider> bookmarkMarkerAttributesProviders) {
		this.bookmarkMarkerAttributesProviders = new ArrayList<>(
				bookmarkMarkerAttributesProviders);
	}

	@Override
	public BookmarkMarker getMarkerDescriptor(Project project, Bookmark bookmark, Optional<IFileBookmarkLocation> fileBookmarkLocation, ProgressIndicator progressIndicator) {
		double progressFraction = (double) 1 / bookmarkMarkerAttributesProviders.size();
		for (IBookmarkMarkerAttributesProvider provider : bookmarkMarkerAttributesProviders) {
			try (SubProgressIndicator subProgressIndicator = new SubProgressIndicator(progressIndicator, progressFraction)) {
				BookmarkMarker bookmarkMarkerDescriptor = provider
						.getMarkerDescriptor(project, bookmark, fileBookmarkLocation, subProgressIndicator);
				if (bookmarkMarkerDescriptor != null) {
					return bookmarkMarkerDescriptor;
				}
			}
		}
		return null;
	}
}
