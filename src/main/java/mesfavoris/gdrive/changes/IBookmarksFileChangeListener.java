package mesfavoris.gdrive.changes;

import com.google.api.services.drive.model.Change;
import com.intellij.util.messages.Topic;
import mesfavoris.model.BookmarkId;

/**
 * Listener interface for bookmarks file change events.
 * Events are published via MessageBus.
 */
public interface IBookmarksFileChangeListener {
	Topic<IBookmarksFileChangeListener> TOPIC = Topic.create("BookmarksFileChangeListener", IBookmarksFileChangeListener.class);

	void bookmarksFileChanged(BookmarkId bookmarkFolderId, Change change);

}
