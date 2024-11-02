package mesfavoris.persistence.json;

import com.google.gson.stream.JsonWriter;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksTreeSerializer;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import static mesfavoris.persistence.json.JsonSerializerConstants.*;

/**
 * Serialize a {@link BookmarksTree}
 * 
 * @author cchabanois
 *
 */
public class BookmarksTreeJsonSerializer implements IBookmarksTreeSerializer {
	private final boolean indent;

	public BookmarksTreeJsonSerializer(boolean indent) {
		this.indent = indent;
	}

	@Override
	public void serialize(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId, Writer writer) throws IOException {
		JsonWriter jsonWriter = new JsonWriter(writer);
        try (jsonWriter) {
            if (indent) {
                jsonWriter.setIndent("  ");
            }
            serialize(bookmarksTree, bookmarkFolderId, jsonWriter);
        }
	}

	private void serialize(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId, JsonWriter writer) throws IOException {
		writer.beginObject();
		writer.name(NAME_VERSION).value(VERSION_1_0);
		writer.name(NAME_BOOKMARKS);
		BookmarkFolder bookmarkFolder = (BookmarkFolder) bookmarksTree.getBookmark(bookmarkFolderId);
		serializeBookmarkFolder(writer, bookmarksTree, bookmarkFolder);
		writer.endObject();
	}

	private void serializeBookmarkFolder(JsonWriter writer, BookmarksTree bookmarksTree, BookmarkFolder bookmarkFolder) throws IOException {
		writer.beginObject();
		writer.name(NAME_ID).value(bookmarkFolder.getId().toString());
		writer.name(NAME_PROPERTIES);
		serializeProperties(writer, bookmarkFolder.getProperties());
		writer.name(NAME_CHILDREN);
		serializeBookmarks(writer, bookmarksTree, bookmarksTree.getChildren(bookmarkFolder.getId()));
		writer.endObject();
	}

	private void serializeProperties(JsonWriter writer, Map<String, String> properties)
			throws IOException {
		writer.beginObject();
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			writer.name(entry.getKey()).value(entry.getValue());
		}
		writer.endObject();
	}

	private void serializeBookmarks(JsonWriter writer, BookmarksTree bookmarksTree, List<Bookmark> bookmarks) throws IOException {
		writer.beginArray();
		for (Bookmark bookmark : bookmarks) {
			if (bookmark instanceof BookmarkFolder) {
				serializeBookmarkFolder(writer, bookmarksTree, (BookmarkFolder) bookmark);
			} else {
				serializeBookmark(writer, bookmarksTree, bookmark);
			}
		}
		writer.endArray();
	}

	private void serializeBookmark(JsonWriter writer, BookmarksTree bookmarksTree, Bookmark bookmark) throws IOException {
		writer.beginObject();
		writer.name(NAME_ID).value(bookmark.getId().toString());
		writer.name(NAME_PROPERTIES);
		serializeProperties(writer, bookmark.getProperties());
		writer.endObject();
	}

}
