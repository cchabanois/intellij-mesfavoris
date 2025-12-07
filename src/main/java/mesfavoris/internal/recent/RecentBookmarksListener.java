package mesfavoris.internal.recent;

import com.intellij.util.messages.Topic;

/**
 * Listener for recent bookmarks changes
 */
public interface RecentBookmarksListener {
    Topic<RecentBookmarksListener> TOPIC = Topic.create("RecentBookmarksListener", RecentBookmarksListener.class);

    void recentBookmarksChanged(RecentBookmarks before, RecentBookmarks after);
}

