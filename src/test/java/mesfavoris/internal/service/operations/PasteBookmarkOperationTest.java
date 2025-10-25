package mesfavoris.internal.service.operations;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.BookmarksException;
import mesfavoris.bookmarktype.IBookmarkPropertiesProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeGenerator;
import mesfavoris.tests.commons.bookmarks.IncrementalIDGenerator;

import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.Map;

import static mesfavoris.tests.commons.bookmarks.BookmarksTreeTestUtil.getBookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeTestUtil.getBookmarkFolder;
import static org.assertj.core.api.Assertions.assertThat;

public class PasteBookmarkOperationTest extends BasePlatformTestCase {
	private BookmarkDatabase bookmarkDatabase;
	private PasteBookmarkOperation pasteBookmarkOperation;
	private IBookmarkPropertiesProvider bookmarkPropertiesProvider = new TestBookmarkPropertiesProvider();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		BookmarksTree bookmarksTree = new BookmarksTreeGenerator(new IncrementalIDGenerator(), 5, 3, 2).build();
		bookmarkDatabase = new BookmarkDatabase("main", bookmarksTree);
		pasteBookmarkOperation = new PasteBookmarkOperation(getProject(), bookmarkDatabase, bookmarkPropertiesProvider);
	}

	public void testPasteBookmarks() throws Exception {
		// Given
		copyToClipboard(getBookmarkFolder(bookmarkDatabase.getBookmarksTree(), 1, 1, 1).getId(),
				getBookmark(bookmarkDatabase.getBookmarksTree(), 2, 2, 2, 2).getId());
		int numberOfBookmarksBefore = bookmarkDatabase.getBookmarksTree().size();

		// When
		pasteBookmarkOperation.paste(getBookmarkFolder(bookmarkDatabase.getBookmarksTree(), 0, 0, 0).getId(),
				new EmptyProgressIndicator());

		// Then
		assertThat(bookmarkDatabase.getBookmarksTree().size()).isEqualTo(numberOfBookmarksBefore + 7);
	}

	public void testPasteInvalidString() throws BookmarksException {
		// Given
		copyToClipboard("Not a bookmark");
		BookmarksTree previousTree = bookmarkDatabase.getBookmarksTree();

		// When
		pasteBookmarkOperation.paste(getBookmarkFolder(bookmarkDatabase.getBookmarksTree(), 0, 0, 0).getId(),
				new EmptyProgressIndicator());

		// Then
		assertThat(bookmarkDatabase.getBookmarksTree()).isEqualTo(previousTree);
	}

	public void testPasteUrl() throws BookmarksException {
		// Given
		copyToClipboard("http://www.google.com");
		int numberOfBookmarksBefore = bookmarkDatabase.getBookmarksTree().size();

		// When
		pasteBookmarkOperation.paste(getBookmarkFolder(bookmarkDatabase.getBookmarksTree(), 0, 0, 0).getId(),
				new EmptyProgressIndicator());

		// Then
		assertThat(bookmarkDatabase.getBookmarksTree().size()).isEqualTo(numberOfBookmarksBefore + 1);
	}
	
	private void copyToClipboard(BookmarkId... bookmarkIds) {
		CopyBookmarkOperation copyBookmarkOperation = new CopyBookmarkOperation();
		copyBookmarkOperation.copyToClipboard(bookmarkDatabase.getBookmarksTree(), Arrays.asList(bookmarkIds));
	}

	private void copyToClipboard(String text) {
		CopyPasteManager.getInstance().setContents(new StringSelection(text));
	}

	private static class TestBookmarkPropertiesProvider implements IBookmarkPropertiesProvider {

		@Override
		public void addBookmarkProperties(Map<String, String> bookmarkProperties, DataContext dataContext,
				ProgressIndicator monitor) {
			// Only create a bookmark if the clipboard contains a URL
			// This simulates what a real bookmark properties provider would do
			String clipboardText = getTextFromClipboard();
			if (clipboardText != null && clipboardText.startsWith("http://")) {
				bookmarkProperties.put(Bookmark.PROPERTY_NAME, clipboardText);
			}
		}

		private String getTextFromClipboard() {
			try {
				return (String) CopyPasteManager.getInstance().getContents().getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
			} catch (Exception e) {
				return null;
			}
		}

	}

}
