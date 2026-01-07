package mesfavoris.internal.service.operations;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.BookmarksException;
import mesfavoris.bookmarktype.IBookmarkPropertiesProvider;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.problems.IBookmarkProblems;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class UpdateBookmarkOperation {
	private final BookmarkDatabase bookmarkDatabase;
	private final IBookmarkPropertiesProvider bookmarkPropertiesProvider;
	private final Supplier<Set<String>> nonUpdatablePropertiesProvider;
	private final IBookmarkProblems bookmarkProblems;

	public UpdateBookmarkOperation(BookmarkDatabase bookmarkDatabase, IBookmarkProblems bookmarkProblems,
			IBookmarkPropertiesProvider bookmarkPropertiesProvider,
			Supplier<Set<String>> nonUpdatablePropertiesProvider) {
		this.bookmarkDatabase = bookmarkDatabase;
		this.bookmarkProblems = bookmarkProblems;
		this.bookmarkPropertiesProvider = bookmarkPropertiesProvider;
		this.nonUpdatablePropertiesProvider = nonUpdatablePropertiesProvider;
	}

	public void updateBookmark(BookmarkId bookmarkId, DataContext dataContext, ProgressIndicator progress)
			throws BookmarksException {
		Map<String, String> bookmarkProperties = new HashMap<>();
		bookmarkPropertiesProvider.addBookmarkProperties(bookmarkProperties, dataContext, progress);
		if (bookmarkProperties.isEmpty()) {
			throw new BookmarksException("Could not update bookmark from current selection");
		}
		updateBookmark(bookmarkId, bookmarkProperties);
	}

	private void updateBookmark(final BookmarkId bookmarkId, Map<String, String> properties) throws BookmarksException {
		Set<String> nonUpdatableProperties = nonUpdatablePropertiesProvider.get();
		bookmarkDatabase.modify(bookmarksTreeModifier -> {
			properties.forEach((propertyName, propertyValue) -> {
				if (!nonUpdatableProperties.contains(propertyName)) {
					bookmarksTreeModifier.setPropertyValue(bookmarkId, propertyName, propertyValue);
				}
			});
		}, bookmarksTree -> bookmarkProblems.delete(bookmarkId));
	}

}
