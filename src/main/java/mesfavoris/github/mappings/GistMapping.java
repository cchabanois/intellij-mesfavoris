package mesfavoris.github.mappings;

import mesfavoris.model.BookmarkId;
import mesfavoris.remote.RemoteBookmarkFolder;

import java.util.Map;
import java.util.Objects;

/** Immutable association between a bookmark folder and the GitHub Gist that stores its content. */
public class GistMapping {
    public static final String PROP_GIST_URL = "gistUrl";
    public static final String PROP_OWNER_LOGIN = "ownerLogin";
    public static final String PROP_BOOKMARKS_COUNT = RemoteBookmarkFolder.PROP_BOOKMARKS_COUNT;

    private final BookmarkId bookmarkFolderId;
    private final String gistId;
    private final Map<String, String> properties;

    public GistMapping(BookmarkId bookmarkFolderId, String gistId, Map<String, String> properties) {
        this.bookmarkFolderId = bookmarkFolderId;
        this.gistId = gistId;
        this.properties = Map.copyOf(properties);
    }

    public BookmarkId getBookmarkFolderId() {
        return bookmarkFolderId;
    }

    public String getGistId() {
        return gistId;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GistMapping that)) return false;
        return Objects.equals(bookmarkFolderId, that.bookmarkFolderId)
                && Objects.equals(gistId, that.gistId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookmarkFolderId, gistId);
    }
}
