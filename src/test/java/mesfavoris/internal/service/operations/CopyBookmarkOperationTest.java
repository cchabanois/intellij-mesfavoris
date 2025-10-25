package mesfavoris.internal.service.operations;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.json.BookmarksTreeJsonDeserializer;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeGenerator;
import mesfavoris.tests.commons.bookmarks.IncrementalIDGenerator;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import static mesfavoris.tests.commons.bookmarks.BookmarksTreeTestUtil.getBookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeTestUtil.getBookmarkFolder;
import static org.assertj.core.api.Assertions.assertThat;

public class CopyBookmarkOperationTest extends BasePlatformTestCase {
	private CopyBookmarkOperation copyBookmarkOperation;
	private BookmarksTree bookmarksTree;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		copyBookmarkOperation = new CopyBookmarkOperation();
		bookmarksTree = new BookmarksTreeGenerator(new IncrementalIDGenerator(), 5, 3, 2).build();
	}

	public void testCopyBookmarksToClipboard() throws Exception {
		// Given
		List<BookmarkId> selection = Arrays.asList(getBookmarkFolder(bookmarksTree, 1, 1, 1).getId(),
				getBookmark(bookmarksTree, 2, 2, 2, 2).getId());

		// When
		copyBookmarkOperation.copyToClipboard(bookmarksTree, selection);
		String clipboardContents = getClipboardContents();

		// Then
		BookmarksTree bookmarksTreeClipboard = deserialize(clipboardContents);
		assertThat(bookmarksTreeClipboard.getChildren(bookmarksTreeClipboard.getRootFolder().getId()).size()).isEqualTo(2);
	}

	private String getClipboardContents() {
		try {
			return (String) CopyPasteManager.getInstance().getContents().getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get clipboard contents", e);
		}
	}

	private BookmarksTree deserialize(String serializedBookmarksTree) throws IOException {
		BookmarksTreeJsonDeserializer deserializer = new BookmarksTreeJsonDeserializer();
		return deserializer.deserialize(new StringReader(serializedBookmarksTree));
	}

}
