package mesfavoris.internal.visited;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import mesfavoris.internal.ui.virtual.BookmarkLink;
import mesfavoris.internal.ui.virtual.VirtualBookmarkFolder;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.internal.visited.VisitedBookmarks;
import mesfavoris.internal.visited.IVisitedBookmarksProvider;
import mesfavoris.internal.visited.VisitedBookmarksListener;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LatestVisitedBookmarksVirtualFolder extends VirtualBookmarkFolder implements VisitedBookmarksListener {
	private final Project project;
	private final BookmarkDatabase bookmarkDatabase;
	private final IVisitedBookmarksProvider visitedBookmarksProvider;
	private final int count;
	private MessageBusConnection messageBusConnection;

	public LatestVisitedBookmarksVirtualFolder(Project project, BookmarkDatabase bookmarkDatabase,
			IVisitedBookmarksProvider visitedBookmarksProvider, BookmarkId parentId, int count) {
		super(parentId, "Latest visited");
		this.project = project;
		this.bookmarkDatabase = bookmarkDatabase;
		this.visitedBookmarksProvider = visitedBookmarksProvider;
		this.count = count;
	}

	@Override
	public void visitedBookmarksChanged(VisitedBookmarks previousVisitedBookmarks, VisitedBookmarks newVisitedBookmarks) {
		if (!previousVisitedBookmarks.getLatestVisitedBookmarks(count).equals(newVisitedBookmarks.getLatestVisitedBookmarks(count))) {
			fireChildrenChanged();
		}
	}

	@Override
	public List<BookmarkLink> getChildren() {
		BookmarksTree bookmarksTree = bookmarkDatabase.getBookmarksTree();
		return visitedBookmarksProvider.getVisitedBookmarks().getLatestVisitedBookmarks(count).stream()
				.map(bookmarksTree::getBookmark)
				.filter(Objects::nonNull)
				.map(bookmark -> new BookmarkLink(bookmarkFolder.getId(), bookmark)).collect(Collectors.toList());
	}

	@Override
	protected void initListening() {
		messageBusConnection = project.getMessageBus().connect();
		messageBusConnection.subscribe(VisitedBookmarksListener.TOPIC, this);
	}

	@Override
	protected void stopListening() {
		if (messageBusConnection != null) {
			messageBusConnection.disconnect();
			messageBusConnection = null;
		}
	}

}