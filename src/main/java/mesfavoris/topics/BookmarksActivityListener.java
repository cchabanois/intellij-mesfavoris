package mesfavoris.topics;

import com.intellij.util.messages.Topic;
import mesfavoris.model.BookmarkId;

public interface BookmarksActivityListener {

    Topic<BookmarksActivityListener> TOPIC = Topic.create("Bookmarks Activity", BookmarksActivityListener.class);

    default void bookmarkVisited(BookmarkId bookmarkId) {
    }


}