package mesfavoris.internal.tags;

import mesfavoris.internal.ui.virtual.BookmarkLink;
import mesfavoris.internal.ui.virtual.IVirtualBookmarkFolderListener;
import mesfavoris.internal.ui.virtual.VirtualBookmarkFolder;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static mesfavoris.tags.TagsBookmarkProperties.PROP_TAGS;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;

public class TagsVirtualFolderTest {
	private final BookmarkId rootId = new BookmarkId("root");
	private final BookmarkId b1 = new BookmarkId("b1");
	private final BookmarkId b2 = new BookmarkId("b2");
	private final BookmarkId b3 = new BookmarkId("b3");
	private BookmarkDatabase bookmarkDatabase;
	private TagsRootVirtualFolder tagsRoot;

	@Before
	public void setUp() {
		BookmarksTree tree = bookmarksTree("root")
				.addBookmarks("root",
						bookmark(b1, "B1").withProperty(PROP_TAGS, "bug,perf"),
						bookmark(b2, "B2").withProperty(PROP_TAGS, "Perf"),
						bookmark(b3, "B3"))
				.build();
		bookmarkDatabase = new BookmarkDatabase("test", tree);
		tagsRoot = new TagsRootVirtualFolder(bookmarkDatabase, new TagsIndex(bookmarkDatabase), rootId);
	}

	@Test
	public void testRootExposesOneFolderPerTag() {
		List<VirtualBookmarkFolder> folders = tagsRoot.getChildFolders();
		assertThat(folders).extracting(f -> ((TagVirtualFolder) f).getTag())
				.containsExactly("bug", "perf");
		assertThat(tagsRoot.getChildren()).isEmpty();
	}

	@Test
	public void testTagFolderListsMatchingBookmarksCaseInsensitively() {
		TagVirtualFolder perf = folderForTag("perf");
		assertThat(perf.getChildren()).extracting(link -> link.getBookmark().getId())
				.containsExactlyInAnyOrder(b1, b2);

		TagVirtualFolder bug = folderForTag("bug");
		assertThat(bug.getChildren()).extracting(link -> link.getBookmark().getId())
				.containsExactly(b1);
	}

	@Test
	public void testChildFoldersReuseInstancesAcrossCalls() {
		VirtualBookmarkFolder before = tagsRoot.getChildFolders().get(0);
		VirtualBookmarkFolder after = tagsRoot.getChildFolders().get(0);
		assertThat(after).isSameAs(before);
	}

	@Test
	public void testFoldersAreRemovedWhenTagDisappears() throws Exception {
		bookmarkDatabase.modify(modifier -> modifier.setPropertyValue(b1, PROP_TAGS, "perf"));
		// "bug" was only on b1; it should be gone now
		assertThat(tagsRoot.getChildFolders()).extracting(f -> ((TagVirtualFolder) f).getTag())
				.containsExactly("perf");
	}

	@Test
	public void testListenerFiresWhenBookmarksChange() throws Exception {
		AtomicInteger count = new AtomicInteger();
		IVirtualBookmarkFolderListener listener = folder -> count.incrementAndGet();
		tagsRoot.addListener(listener);

		bookmarkDatabase.modify(modifier -> modifier.setPropertyValue(b3, PROP_TAGS, "ui"));

		assertThat(count.get()).isGreaterThanOrEqualTo(1);
		assertThat(tagsRoot.getChildFolders()).extracting(f -> ((TagVirtualFolder) f).getTag())
				.contains("ui");
	}

	private TagVirtualFolder folderForTag(String tag) {
		return (TagVirtualFolder) tagsRoot.getChildFolders().stream()
				.filter(f -> ((TagVirtualFolder) f).getTag().equalsIgnoreCase(tag))
				.findFirst().orElseThrow();
	}
}
