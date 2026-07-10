package mesfavoris.internal.tags;

import mesfavoris.internal.ui.virtual.BookmarkLink;
import mesfavoris.internal.ui.virtual.VirtualBookmarkFolder;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Virtual folder listing every leaf bookmark carrying a given tag. Instances are created and refreshed by
 * {@link TagsRootVirtualFolder}, which owns the database listening; this folder therefore does not listen
 * on its own.
 */
public class TagVirtualFolder extends VirtualBookmarkFolder {
	private final BookmarkDatabase bookmarkDatabase;
	private final TagsIndex tagsIndex;
	private final String tag;

	public TagVirtualFolder(BookmarkId parentId, BookmarkDatabase bookmarkDatabase, TagsIndex tagsIndex, String tag) {
		super(parentId, tag);
		this.bookmarkDatabase = bookmarkDatabase;
		this.tagsIndex = tagsIndex;
		this.tag = tag;
	}

	public String getTag() {
		return tag;
	}

	@Override
	public List<BookmarkLink> getChildren() {
		BookmarksTree bookmarksTree = bookmarkDatabase.getBookmarksTree();
		List<BookmarkLink> links = new ArrayList<>();
		for (BookmarkId bookmarkId : tagsIndex.getBookmarkIds(tag)) {
			Bookmark bookmark = bookmarksTree.getBookmark(bookmarkId);
			if (bookmark == null || bookmark instanceof BookmarkFolder) {
				continue;
			}
			links.add(new BookmarkLink(bookmarkFolder.getId(), bookmark));
		}
		return links;
	}

	@Override
	protected void initListening() {
		// no-op: refresh is driven by the owning TagsRootVirtualFolder
	}

	@Override
	protected void stopListening() {
		// no-op
	}
}
