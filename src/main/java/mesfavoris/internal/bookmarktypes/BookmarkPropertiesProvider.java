package mesfavoris.internal.bookmarktypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.progress.ProgressIndicator;

import mesfavoris.bookmarktype.IBookmarkPropertiesProvider;
import mesfavoris.commons.SubProgressIndicator;

public class BookmarkPropertiesProvider implements IBookmarkPropertiesProvider {
	private final List<IBookmarkPropertiesProvider> providers;

	public BookmarkPropertiesProvider(List<IBookmarkPropertiesProvider> providers) {
		this.providers = new ArrayList<>(providers);
	}

	@Override
	public void addBookmarkProperties(Map<String, String> bookmarkProperties, DataContext dataContext, ProgressIndicator progress) {
			double progressFraction = (double) 1 /providers.size();
			providers.forEach(p -> {
				try(SubProgressIndicator subProgressIndicator = new SubProgressIndicator(progress, progressFraction)) {
					p.addBookmarkProperties(bookmarkProperties, dataContext, subProgressIndicator);
				}
			});
	}

}
