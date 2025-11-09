package mesfavoris.remote;

import com.intellij.util.messages.Topic;
import mesfavoris.model.BookmarkId;

/**
 * Listener interface for remote bookmarks store events
 */
public interface RemoteBookmarksStoreListener {
	Topic<RemoteBookmarksStoreListener> TOPIC = Topic.create("RemoteBookmarksStoreListener", RemoteBookmarksStoreListener.class);

	void remoteBookmarksStoreConnected(String remoteBookmarksStoreId);

	void remoteBookmarksStoreDisconnected(String remoteBookmarksStoreId);

	void mappingAdded(String remoteBookmarksStoreId, BookmarkId bookmarkFolderId);

	void mappingRemoved(String remoteBookmarksStoreId, BookmarkId bookmarkFolderId);

	void remoteBookmarksTreeChanged(String remoteBookmarksStoreId, BookmarkId bookmarkFolderId);
}

