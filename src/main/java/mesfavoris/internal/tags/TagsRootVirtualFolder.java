package mesfavoris.internal.tags;

import mesfavoris.internal.ui.virtual.BookmarkLink;
import mesfavoris.internal.ui.virtual.VirtualBookmarkFolder;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.IBookmarksListener;
import mesfavoris.tags.Tags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * Root virtual folder grouping bookmarks by tag: it exposes one {@link TagVirtualFolder} per tag currently
 * in use across the tree. It listens to the bookmark database and refreshes the whole tags subtree whenever
 * bookmarks change (the set of tags or their membership may have changed).
 */
public class TagsRootVirtualFolder extends VirtualBookmarkFolder {
	private final BookmarkDatabase bookmarkDatabase;
	// reuse TagVirtualFolder instances across refreshes to keep tree node identity/expansion stable
	private final Map<String, TagVirtualFolder> tagFolders = new LinkedHashMap<>();
	private final IBookmarksListener bookmarksListener = modifications -> fireChildrenChanged();

	public TagsRootVirtualFolder(BookmarkDatabase bookmarkDatabase, BookmarkId parentId) {
		super(parentId, "Tags");
		this.bookmarkDatabase = bookmarkDatabase;
	}

	@Override
	public List<BookmarkLink> getChildren() {
		// no direct bookmarks; children are the per-tag sub-folders
		return List.of();
	}

	@Override
	public synchronized List<VirtualBookmarkFolder> getChildFolders() {
		SortedSet<String> tags = Tags.collectAllTags(bookmarkDatabase.getBookmarksTree());
		// drop folders for tags that no longer exist (tags is case-insensitive)
		tagFolders.keySet().removeIf(tag -> !tags.contains(tag));
		List<VirtualBookmarkFolder> result = new ArrayList<>();
		for (String tag : tags) {
			result.add(tagFolders.computeIfAbsent(tag,
					t -> new TagVirtualFolder(bookmarkFolder.getId(), bookmarkDatabase, t)));
		}
		return result;
	}

	@Override
	protected void initListening() {
		bookmarkDatabase.addListener(bookmarksListener);
	}

	@Override
	protected void stopListening() {
		bookmarkDatabase.removeListener(bookmarksListener);
	}
}
