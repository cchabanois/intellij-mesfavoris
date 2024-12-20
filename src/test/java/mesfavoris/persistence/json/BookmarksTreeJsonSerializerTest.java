package mesfavoris.persistence.json;

import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeGenerator;
import mesfavoris.tests.commons.bookmarks.IncrementalIDGenerator;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static mesfavoris.tests.commons.bookmarks.BookmarksTreeTestUtil.getBookmarkFolder;
import static org.junit.Assert.assertEquals;

public class BookmarksTreeJsonSerializerTest {
	private BookmarksTreeJsonSerializer bookmarksTreeJsonSerializer;

	@Before
	public void setUp() {
		bookmarksTreeJsonSerializer = new BookmarksTreeJsonSerializer(true);
	}

	@Test
	public void testSerializeBookmarksTree() throws IOException {
		// Given
		BookmarksTree bookmarksTree = new BookmarksTreeGenerator(new IncrementalIDGenerator(), 5, 3, 2).build();

		// When
		String result = serialize(bookmarksTree, bookmarksTree.getRootFolder().getId());

		// Then
		assertEquals(bookmarksTree.toString(), deserialize(result).toString());
	}

	@Test
	public void testSerializeBookmarksSubTree() throws IOException {
		// Given
		BookmarksTree bookmarksTree = new BookmarksTreeGenerator(new IncrementalIDGenerator(), 5, 3, 2).build();
		BookmarkFolder parentFolder = getBookmarkFolder(bookmarksTree, 0, 0);

		// When
		String result = serialize(bookmarksTree, parentFolder.getId());

		// Then
		assertEquals(bookmarksTree.subTree(parentFolder.getId()).toString(), deserialize(result).toString());
	}

	private String serialize(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId) throws IOException {
		StringWriter writer = new StringWriter();
		bookmarksTreeJsonSerializer.serialize(bookmarksTree, bookmarkFolderId, writer);
		return writer.toString();
	}

	private BookmarksTree deserialize(String serializedBookmarks) throws IOException {
		BookmarksTreeJsonDeserializer deserializer = new BookmarksTreeJsonDeserializer();
		return deserializer.deserialize(new StringReader(serializedBookmarks));
	}

}
