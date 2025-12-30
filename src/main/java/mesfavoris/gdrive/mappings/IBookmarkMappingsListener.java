package mesfavoris.gdrive.mappings;

import com.intellij.util.messages.Topic;
import mesfavoris.model.BookmarkId;

/**
 * Listener interface for bookmark mappings events.
 * Events are published via MessageBus.
 */
public interface IBookmarkMappingsListener {
	Topic<IBookmarkMappingsListener> TOPIC = Topic.create("BookmarkMappingsListener", IBookmarkMappingsListener.class);

	void mappingAdded(BookmarkId bookmarkFolderId);

	void mappingRemoved(BookmarkId bookmarkFolderId);
}
