package mesfavoris.persistence.json;

import static mesfavoris.persistence.json.JsonSerializerConstants.NAME_BOOKMARKS;
import static mesfavoris.persistence.json.JsonSerializerConstants.NAME_CHILDREN;
import static mesfavoris.persistence.json.JsonSerializerConstants.NAME_ID;
import static mesfavoris.persistence.json.JsonSerializerConstants.NAME_PROPERTIES;
import static mesfavoris.persistence.json.JsonSerializerConstants.NAME_VERSION;
import static mesfavoris.persistence.json.JsonSerializerConstants.VERSION_1_0;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.stream.JsonReader;

import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksTreeDeserializer;

/**
 * Deserialize a {@link BookmarksTree}
 *
 * @author cchabanois
 */
public class BookmarksTreeJsonDeserializer implements IBookmarksTreeDeserializer {

    @Override
    public BookmarksTree deserialize(Reader reader) throws IOException {
        try (JsonReader jsonReader = new JsonReader(reader)) {
            return deserialize(jsonReader);
        }
    }

    private BookmarksTree deserialize(JsonReader reader) throws IOException {
        reader.beginObject();
        BookmarksTree bookmarksTree = null;
        if (!NAME_VERSION.equals(reader.nextName())) {
            throw new IOException("Invalid format");
        }
        if (!VERSION_1_0.equals(reader.nextString())) {
            throw new IOException("Invalid format : unknown version");
        }
        if (!NAME_BOOKMARKS.equals(reader.nextName())) {
            throw new IOException("Invalid format");
        }
        bookmarksTree = deserializeBookmarksTree(reader);
        reader.endObject();
        return bookmarksTree;
    }

    private BookmarksTree deserializeBookmarksTree(JsonReader reader) throws IOException {
        reader.beginObject();
        BookmarkId id = null;
        Map<String, String> properties = Collections.emptyMap();
        boolean isFolder = false;

        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(NAME_ID)) {
                id = new BookmarkId(reader.nextString());
            } else if (name.equals(NAME_CHILDREN)) {
                isFolder = true;
                break;
            } else if (name.equals(NAME_PROPERTIES)) {
                properties = deserializeProperties(reader);
            } else {
                reader.skipValue();
            }
        }
        if (!isFolder || id == null) {
            throw new IOException("Invalid format");
        }
        BookmarkFolder bookmarkFolder = new BookmarkFolder(id, properties);
        BookmarksTree bookmarksTree = new BookmarksTree(bookmarkFolder);
        bookmarksTree = deserializeBookmarksArray(reader, bookmarksTree, bookmarkFolder.getId());
        reader.endObject();
        return bookmarksTree;
    }

    private BookmarksTree deserializeBookmark(JsonReader reader, BookmarksTree bookmarksTree, BookmarkId parentId) throws IOException {
        reader.beginObject();
        BookmarkId id = null;
        Map<String, String> properties = Collections.emptyMap();
        boolean isFolder = false;

        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(NAME_ID)) {
                id = new BookmarkId(reader.nextString());
            } else if (name.equals(NAME_CHILDREN)) {
                isFolder = true;
                break;
            } else if (name.equals(NAME_PROPERTIES)) {
                properties = deserializeProperties(reader);
            } else {
                reader.skipValue();
            }
        }
        if (isFolder) {
            BookmarkFolder bookmarkFolder = new BookmarkFolder(id, properties);
            bookmarksTree = bookmarksTree.addBookmarks(parentId, Arrays.<Bookmark>asList(bookmarkFolder));
            bookmarksTree = deserializeBookmarksArray(reader, bookmarksTree, bookmarkFolder.getId());
        } else {
            Bookmark bookmark = new Bookmark(id, properties);
            bookmarksTree = bookmarksTree.addBookmarks(parentId, Arrays.asList(bookmark));
        }
        reader.endObject();
        return bookmarksTree;
    }

    private BookmarksTree deserializeBookmarksArray(JsonReader reader, BookmarksTree bookmarksTree, BookmarkId parentId) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            bookmarksTree = deserializeBookmark(reader, bookmarksTree, parentId);
        }
        reader.endArray();
        return bookmarksTree;
    }

    private Map<String, String> deserializeProperties(JsonReader reader) throws IOException {
        Map<String, String> properties = new TreeMap<String, String>();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            String value = reader.nextString();
            properties.put(name, value);
        }
        reader.endObject();
        return properties;
    }

}
