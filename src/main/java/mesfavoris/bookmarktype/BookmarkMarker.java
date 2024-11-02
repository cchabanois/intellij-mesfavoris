package mesfavoris.bookmarktype;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.intellij.openapi.vfs.VirtualFile;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;

import java.util.Map;
import java.util.Objects;

/**
 * Descriptor used to create a marker for a {@link Bookmark}
 * 
 * @author cchabanois
 *
 */
public class BookmarkMarker {
	public static final String LINE_NUMBER = "lineNumber";
	private final VirtualFile resource;
	private final BookmarkId bookmarkId;
	private final Map<String, String> attributes;

	public BookmarkMarker(VirtualFile resource, BookmarkId bookmarkId, Map<String, String> attributes) {
		this.resource = resource;
		this.bookmarkId = bookmarkId;
		this.attributes = ImmutableMap.copyOf(attributes);
	}

	public BookmarkId getBookmarkId() {
		return bookmarkId;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public VirtualFile getResource() {
		return resource;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BookmarkMarker that = (BookmarkMarker) o;
		return Objects.equals(bookmarkId, that.bookmarkId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bookmarkId);
	}
}
