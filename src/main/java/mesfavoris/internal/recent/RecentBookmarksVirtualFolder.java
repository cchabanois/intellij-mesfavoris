package mesfavoris.internal.recent;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import mesfavoris.internal.ui.virtual.BookmarkLink;
import mesfavoris.internal.ui.virtual.VirtualBookmarkFolder;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RecentBookmarksVirtualFolder extends VirtualBookmarkFolder {
	private final Project project;
	private final BookmarkDatabase bookmarkDatabase;
	private final RecentBookmarksDatabase recentBookmarksDatabase;
	private final int count;
	private MessageBusConnection messageBusConnection;

	public RecentBookmarksVirtualFolder(Project project, BookmarkDatabase bookmarkDatabase,
			RecentBookmarksDatabase recentBookmarksDatabase, BookmarkId parentId, int count) {
		super(parentId, "Recent bookmarks");
		this.project = project;
		this.bookmarkDatabase = bookmarkDatabase;
		this.recentBookmarksDatabase = recentBookmarksDatabase;
		this.count = count;
	}

	private void recentBookmarksChanged(RecentBookmarks before, RecentBookmarks after) {
		if (!before.getRecentBookmarks(count).equals(after.getRecentBookmarks(count))) {
			fireChildrenChanged();
		}
	}

	@Override
	public List<BookmarkLink> getChildren() {
		BookmarksTree bookmarksTree = bookmarkDatabase.getBookmarksTree();
		return recentBookmarksDatabase.getRecentBookmarks().getRecentBookmarks(count).stream()
				.map(bookmarksTree::getBookmark)
				.filter(Objects::nonNull)
				.map(bookmark -> new BookmarkLink(bookmarkFolder.getId(), bookmark))
				.collect(Collectors.toList());
	}

	@Override
	protected void initListening() {
		messageBusConnection = project.getMessageBus().connect();
		messageBusConnection.subscribe(RecentBookmarksListener.TOPIC, this::recentBookmarksChanged);
	}

	@Override
	protected void stopListening() {
		if (messageBusConnection != null) {
			messageBusConnection.disconnect();
			messageBusConnection = null;
		}
	}

}
