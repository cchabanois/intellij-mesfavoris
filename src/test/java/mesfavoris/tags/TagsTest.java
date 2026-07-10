package mesfavoris.tags;

import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static mesfavoris.tags.TagsBookmarkProperties.PROP_TAGS;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.junit.Assert.*;

public class TagsTest {

	@Test
	public void testParseSplitsTrimsAndDropsEmpty() {
		assertEquals(List.of("bug", "perf"), Tags.parse(" bug ,  , perf "));
	}

	@Test
	public void testParseDeduplicatesCaseInsensitivelyKeepingFirst() {
		assertEquals(List.of("Bug", "perf"), Tags.parse("Bug, bug, perf, PERF"));
	}

	@Test
	public void testParseNullOrBlankIsEmpty() {
		assertTrue(Tags.parse(null).isEmpty());
		assertTrue(Tags.parse("   ").isEmpty());
	}

	@Test
	public void testFormatSortsCaseInsensitivelyAndDeduplicates() {
		// case-insensitive sort + dedup; first-seen casing wins ("PERF" before "perf")
		assertEquals("bug,PERF,zebra", Tags.format(List.of("zebra", "bug", "PERF", "perf")));
	}

	@Test
	public void testFormatEmptyReturnsNull() {
		assertNull(Tags.format(List.of()));
		assertNull(Tags.format(List.of("  ")));
	}

	@Test
	public void testAddTag() {
		assertEquals("bug,perf", Tags.addTag("perf", "bug"));
		// already present (case-insensitive) -> no-op
		assertEquals("perf", Tags.addTag("perf", "PERF"));
		assertEquals("bug", Tags.addTag(null, "bug"));
	}

	@Test
	public void testRemoveTag() {
		assertEquals("perf", Tags.removeTag("bug,perf", "BUG"));
		assertNull(Tags.removeTag("bug", "bug"));
		assertEquals("perf", Tags.removeTag("perf", "absent"));
	}

	@Test
	public void testGetTags() {
		Bookmark bookmark = new Bookmark(new BookmarkId("b"), Map.of(PROP_TAGS, "bug, perf"));
		assertEquals(List.of("bug", "perf"), Tags.getTags(bookmark));
		assertTrue(Tags.getTags(new Bookmark(new BookmarkId("b2"))).isEmpty());
	}

	@Test
	public void testHasTagMatching() {
		Bookmark bookmark = new Bookmark(new BookmarkId("b"), Map.of(PROP_TAGS, "bug,performance"));
		assertTrue(Tags.hasTagMatching(bookmark, "perf"));
		assertTrue(Tags.hasTagMatching(bookmark, "BUG"));
		assertFalse(Tags.hasTagMatching(bookmark, "zebra"));
		// blank term matches any tagged bookmark
		assertTrue(Tags.hasTagMatching(bookmark, ""));
		assertFalse(Tags.hasTagMatching(new Bookmark(new BookmarkId("b2")), ""));
	}

	@Test
	public void testExtractTagQuery() {
		assertEquals("bug", Tags.extractTagQuery("tag:bug"));
		assertEquals("bug", Tags.extractTagQuery("TAG: Bug "));
		assertEquals("bug", Tags.extractTagQuery("#bug"));
		assertNull(Tags.extractTagQuery("bug"));
		assertNull(Tags.extractTagQuery(null));
	}

	@Test
	public void testCollectAllTags() {
		BookmarksTree tree = bookmarksTree("root")
				.addBookmarks("root",
						bookmark(new BookmarkId("b1"), "b1").withProperty(PROP_TAGS, "bug, perf"),
						bookmark(new BookmarkId("b2"), "b2").withProperty(PROP_TAGS, "Perf, ui"),
						bookmark(new BookmarkId("b3"), "b3"))
				.build();
		SortedSet<String> allTags = Tags.collectAllTags(tree);
		assertEquals(List.of("bug", "perf", "ui"), List.copyOf(allTags));
	}
}
