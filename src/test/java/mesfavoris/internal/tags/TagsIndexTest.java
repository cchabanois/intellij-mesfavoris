package mesfavoris.internal.tags;

import mesfavoris.BookmarksException;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.modification.IBookmarksTreeModifier;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static mesfavoris.tags.TagsBookmarkProperties.PROP_TAGS;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;

public class TagsIndexTest {
	private final BookmarkId rootId = new BookmarkId("root");
	private final BookmarkId folderId = new BookmarkId("folder");
	private final BookmarkId b1 = new BookmarkId("b1");
	private final BookmarkId b2 = new BookmarkId("b2");
	private mesfavoris.model.BookmarkDatabase bookmarkDatabase;
	private TagsIndex tagsIndex;

	@Before
	public void setUp() {
		BookmarksTree tree = bookmarksTree("root")
				.addBookmarks("root",
						bookmark(b1, "B1").withProperty(PROP_TAGS, "bug,perf"),
						bookmark(b2, "B2").withProperty(PROP_TAGS, "Perf"))
				.build();
		bookmarkDatabase = new mesfavoris.model.BookmarkDatabase("test", tree);
		tagsIndex = new TagsIndex(bookmarkDatabase);
	}

	@Test
	public void testInitialBuild() {
		assertThat(tagsIndex.getAllTags()).containsExactly("bug", "perf");
		assertThat(tagsIndex.getBookmarkIds("perf")).containsExactlyInAnyOrder(b1, b2);
		assertThat(tagsIndex.getBookmarkIds("bug")).containsExactly(b1);
	}

	@Test
	public void testGetBookmarkIdsIsCaseInsensitive() {
		assertThat(tagsIndex.getBookmarkIds("PERF")).containsExactlyInAnyOrder(b1, b2);
	}

	@Test
	public void testAddingBookmarkIndexesItsTags() throws BookmarksException {
		tagsIndex.getAllTags(); // force initial build so the add goes through the event path
		modify(m -> m.addBookmarks(rootId, List.of(
				bookmark(new BookmarkId("b3"), "B3").withProperty(PROP_TAGS, "ui").build())));

		assertThat(tagsIndex.getAllTags()).contains("ui");
		assertThat(tagsIndex.getBookmarkIds("ui")).containsExactly(new BookmarkId("b3"));
	}

	@Test
	public void testChangingTagsPropertyUpdatesIndex() throws BookmarksException {
		tagsIndex.getAllTags();
		modify(m -> m.setPropertyValue(b1, PROP_TAGS, "perf,ui"));

		// "bug" was only on b1 -> gone; "ui" added; b1 still under "perf"
		assertThat(tagsIndex.getBookmarkIds("bug")).isEmpty();
		assertThat(tagsIndex.getBookmarkIds("ui")).containsExactly(b1);
		assertThat(tagsIndex.getBookmarkIds("perf")).containsExactlyInAnyOrder(b1, b2);
	}

	@Test
	public void testRemovingLastBookmarkForTagRemovesTag() throws BookmarksException {
		tagsIndex.getAllTags();
		modify(m -> m.setPropertyValue(b1, PROP_TAGS, "perf"));

		assertThat(tagsIndex.getAllTags()).doesNotContain("bug");
	}

	@Test
	public void testDeletingBookmarkRemovesItsTags() throws BookmarksException {
		tagsIndex.getAllTags();
		modify(m -> m.deleteBookmark(b1, false));

		assertThat(tagsIndex.getBookmarkIds("bug")).isEmpty();
		assertThat(tagsIndex.getBookmarkIds("perf")).containsExactly(b2);
	}

	@Test
	public void testDeletingFolderRecursivelyRemovesDescendantTags() throws BookmarksException {
		// move b1,b2 under a folder, then delete the folder recursively
		BookmarksTree tree = bookmarksTree("root")
				.addBookmarks("root", bookmarkFolder(folderId, "folder"))
				.addBookmarks(folderId,
						bookmark(b1, "B1").withProperty(PROP_TAGS, "bug"),
						bookmark(b2, "B2").withProperty(PROP_TAGS, "perf"))
				.build();
		bookmarkDatabase = new mesfavoris.model.BookmarkDatabase("test", tree);
		tagsIndex = new TagsIndex(bookmarkDatabase);
		assertThat(tagsIndex.getAllTags()).containsExactly("bug", "perf");

		modify(m -> m.deleteBookmark(folderId, true));

		assertThat(tagsIndex.getAllTags()).isEmpty();
	}

	private void modify(java.util.function.Consumer<IBookmarksTreeModifier> operation) throws BookmarksException {
		bookmarkDatabase.modify(operation::accept);
	}
}
