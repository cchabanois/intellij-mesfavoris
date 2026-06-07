package mesfavoris.persistence;

import com.intellij.util.messages.Topic;
import mesfavoris.model.BookmarkId;

import java.util.Set;

public interface IBookmarksDirtyStateListener {
	Topic<IBookmarksDirtyStateListener> TOPIC =
			Topic.create("BookmarksDirtyStateListener", IBookmarksDirtyStateListener.class);

	void dirtyBookmarks(Set<BookmarkId> dirtyBookmarks);
}
