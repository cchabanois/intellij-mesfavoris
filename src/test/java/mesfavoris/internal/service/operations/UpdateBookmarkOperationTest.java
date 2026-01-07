package mesfavoris.internal.service.operations;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import mesfavoris.bookmarktype.IBookmarkPropertiesProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.problems.IBookmarkProblems;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.Map;
import java.util.Set;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;

public class UpdateBookmarkOperationTest {
	private UpdateBookmarkOperation updateBookmarkOperation;
	private BookmarkDatabase bookmarkDatabase;
	private IBookmarkPropertiesProvider bookmarkPropertiesProvider = mock(IBookmarkPropertiesProvider.class);
	private Set<String> nonUpdatableProperties = Sets.newHashSet(Bookmark.PROPERTY_NAME, Bookmark.PROPERTY_COMMENT,
			Bookmark.PROPERTY_CREATED);
	private IBookmarkProblems bookmarkProblems = mock(IBookmarkProblems.class);

	@Before
	public void setUp() {
		bookmarkDatabase = new BookmarkDatabase("test", createBookmarksTree());
		updateBookmarkOperation = new UpdateBookmarkOperation(bookmarkDatabase, bookmarkProblems,
				bookmarkPropertiesProvider, () -> nonUpdatableProperties);
	}

	@Test
	public void testUpdateDoesNotUpdateUserEditableProperties() throws Exception {
		// Given
		DataContext dataContext = mock(DataContext.class);
		doPutPropertiesWhenAddBookmarkPropertiesCalled(bookmarkPropertiesProvider, dataContext,
				ImmutableMap.of(Bookmark.PROPERTY_NAME, "bookmark12 renamed", Bookmark.PROPERTY_COMMENT,
						"comment for bookmark12 modified", "customProperty", "custom value modified", "newProperty",
						"newCustomValue"));

		// When
		updateBookmarkOperation.updateBookmark(new BookmarkId("bookmark12"), dataContext,
				new ProgressIndicatorBase());

		// Then
		Bookmark bookmark = bookmarkDatabase.getBookmarksTree().getBookmark(new BookmarkId("bookmark12"));
		assertEquals("bookmark12", bookmark.getPropertyValue(Bookmark.PROPERTY_NAME));
		assertEquals("comment for bookmark12", bookmark.getPropertyValue(Bookmark.PROPERTY_COMMENT));
		assertEquals("custom value modified", bookmark.getPropertyValue("customProperty"));
		assertEquals("newCustomValue", bookmark.getPropertyValue("newProperty"));
	}

	private BookmarksTree createBookmarksTree() {
		BookmarksTreeBuilder bookmarksTreeBuilder = bookmarksTree("root");
		bookmarksTreeBuilder.addBookmarks("root", bookmarkFolder("folder1"), bookmarkFolder("folder2"));
		bookmarksTreeBuilder.addBookmarks("folder1", bookmarkFolder("folder11"), bookmark("bookmark11"),
				bookmark("bookmark12").withProperty(Bookmark.PROPERTY_COMMENT, "comment for bookmark12")
						.withProperty("customProperty", "custom value"));

		return bookmarksTreeBuilder.build();
	}

	private void doPutPropertiesWhenAddBookmarkPropertiesCalled(IBookmarkPropertiesProvider mock,
			DataContext dataContext, Map<String, String> propertiesToAdd) {
		Mockito.doAnswer((Answer<Void>) invocation -> {
			Object[] args = invocation.getArguments();
			Map<String, String> bookmarkProperties = (Map<String, String>) args[0];
			bookmarkProperties.putAll(propertiesToAdd);
			return null;
		}).when(mock).addBookmarkProperties(anyMap(), eq(dataContext), any(ProgressIndicator.class));
	}

}
