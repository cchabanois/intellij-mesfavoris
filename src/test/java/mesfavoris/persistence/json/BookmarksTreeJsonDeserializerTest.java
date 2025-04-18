package mesfavoris.persistence.json;

import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeGenerator;
import mesfavoris.tests.commons.bookmarks.IncrementalIDGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class BookmarksTreeJsonDeserializerTest {
	private final BookmarksTreeJsonDeserializer deserializer = new BookmarksTreeJsonDeserializer();

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testDeserializeBookmarksTree() throws IOException {
		// Given
		BookmarksTree bookmarksTree = new BookmarksTreeGenerator(new IncrementalIDGenerator(), 5, 3, 2).build();
		String serializedBookmarks = serialize(bookmarksTree, bookmarksTree.getRootFolder().getId());

		// When
		BookmarksTree deserializedBookmarksTree = deserialize(serializedBookmarks);

		// Then
		assertEquals(bookmarksTree.toString(), deserializedBookmarksTree.toString());
	}

	@Test
	public void testCannotDeserializeBookmarksTreeWithNewerVersion() throws IOException {
		// Given
		BookmarksTree bookmarksTree = new BookmarksTreeGenerator(new IncrementalIDGenerator(), 5, 3, 2).build();
		String serializedBookmarks = serialize(bookmarksTree, bookmarksTree.getRootFolder().getId());

		// When/Then
		expectedException.expect(IOException.class);
		expectedException.expectMessage("Invalid format : unknown version");
		serializedBookmarks = serializedBookmarks.replace("1.0", "99.0");
		deserialize(serializedBookmarks);
	}

	private String serialize(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId) throws IOException {
		StringWriter writer = new StringWriter();
		BookmarksTreeJsonSerializer bookmarksTreeJsonSerializer = new BookmarksTreeJsonSerializer(true);
		bookmarksTreeJsonSerializer.serialize(bookmarksTree, bookmarkFolderId, writer);
		return writer.toString();
	}

	private BookmarksTree deserialize(String serializedBookmarks) throws IOException {
		return deserializer.deserialize(new StringReader(serializedBookmarks));
	}

}
