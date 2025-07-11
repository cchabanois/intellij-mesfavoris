package mesfavoris.internal.service.operations;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import mesfavoris.BookmarksException;
import mesfavoris.internal.placeholders.PathPlaceholdersMap;
import mesfavoris.model.*;
import mesfavoris.placeholders.PathPlaceholder;
import mesfavoris.texteditor.TextEditorBookmarkProperties;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_FILE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class ExpandBookmarksOperationTest {

	private final PathPlaceholdersMap pathPlaceholders = new PathPlaceholdersMap();
	private ExpandBookmarksOperation expandBookmarksOperation;
	private final List<String> pathPropertyNames = Lists.newArrayList(TextEditorBookmarkProperties.PROP_FILE_PATH);

	@Before
	public void setUp() {
		pathPlaceholders.add(new PathPlaceholder("HOME", Paths.get("/home/cchabanois")));
		pathPlaceholders.add(new PathPlaceholder("BLT", Paths.get("/home/cchabanois/blt")));
	}

	private BookmarkDatabase getBookmarkDatabase(Bookmark... bookmarks) {
		BookmarkFolder rootFolder = new BookmarkFolder(new BookmarkId("rootId"), "root");
		BookmarksTree bookmarksTree = new BookmarksTree(rootFolder);
		bookmarksTree = bookmarksTree.addBookmarks(rootFolder.getId(), Lists.newArrayList(bookmarks));
		BookmarkDatabase bookmarkDatabase = new BookmarkDatabase("id", bookmarksTree);
		return bookmarkDatabase;
	}

	@Test
	public void testExpand() throws BookmarksException {
		// Given
		Bookmark bookmark = bookmark("bookmark", "${BLT}/myfile.txt");
		BookmarkDatabase bookmarkDatabase = getBookmarkDatabase(bookmark);
		expandBookmarksOperation = new ExpandBookmarksOperation(bookmarkDatabase, pathPlaceholders, pathPropertyNames);

		// When
		expandBookmarksOperation.expand(Lists.newArrayList(bookmark.getId()));

		// Then
		assertFilePath(bookmarkDatabase.getBookmarksTree(), bookmark.getId(), "/home/cchabanois/blt/myfile.txt");
	}

	private Bookmark bookmark(String name, String filePath) {
		Map<String, String> properties = Maps.newHashMap();
		properties.put(Bookmark.PROPERTY_NAME, name);
		properties.put(PROP_FILE_PATH, filePath);
		return new Bookmark(new BookmarkId(), properties);
	}

	private void assertFilePath(BookmarksTree bookmarksTree, BookmarkId bookmarkId, String expectedFilePath) {
		Bookmark bookmark = bookmarksTree.getBookmark(bookmarkId);
		assertThat(bookmark).isNotNull();
		assertThat(bookmark.getPropertyValue(PROP_FILE_PATH)).isEqualTo(expectedFilePath);
	}

}
