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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

	@Test
	public void testDuplicateBookmarkIdIsInvalidFormat() {
		String json = """
				{"version":"1.0","bookmarks":{"id":"root","children":[
				  {"id":"b1","properties":{"name":"one"}},
				  {"id":"b1","properties":{"name":"two"}}]}}""";

		assertThatThrownBy(() -> deserialize(json)).isInstanceOf(IOException.class)
				.hasMessage("Invalid format : duplicate bookmark id b1 at path $.bookmarks.children[1]");
	}

	@Test
	public void testBookmarkWithoutIdIsInvalidFormat() {
		String json = """
				{"version":"1.0","bookmarks":{"id":"root","children":[
				  {"properties":{"name":"one"}}]}}""";

		assertThatThrownBy(() -> deserialize(json)).isInstanceOf(IOException.class)
				.hasMessage("Invalid format : bookmark without id at path $.bookmarks.children[0]");
	}

	@Test
	public void testUnexpectedTokenIsInvalidFormat() {
		String json = """
				{"version":"1.0","bookmarks":{"id":"root","children":{"id":"b1"}}}""";

		assertThatThrownBy(() -> deserialize(json)).isInstanceOf(IOException.class)
				.hasMessageContaining("Invalid format : Expected BEGIN_ARRAY but was BEGIN_OBJECT");
	}

	@Test
	public void testTruncatedFileIsIOException() {
		String json = """
				{"version":"1.0","bookmarks":{"id":"root","children":[
				  {"id":"b1","properties":{"name":"o""";

		assertThatThrownBy(() -> deserialize(json)).isInstanceOf(IOException.class);
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
