package mesfavoris.internal.visited;

import com.intellij.util.messages.Topic;
import mesfavoris.internal.recent.RecentBookmarksListener;

public interface VisitedBookmarksListener {

	Topic<VisitedBookmarksListener> TOPIC = Topic.create("Visited Bookmarks", VisitedBookmarksListener.class);

	void visitedBookmarksChanged(VisitedBookmarks previousVisitedBookmarks, VisitedBookmarks newVisitedBookmarks);
}