package mesfavoris.github.mappings;

import com.intellij.util.messages.Topic;
import mesfavoris.model.BookmarkId;

/** MessageBus topic fired when a BookmarkFolder → Gist mapping is added or removed. */
public interface IGistMappingsListener {
    Topic<IGistMappingsListener> TOPIC =
            Topic.create("GistMappingsListener", IGistMappingsListener.class);

    void mappingAdded(BookmarkId bookmarkFolderId);

    void mappingRemoved(BookmarkId bookmarkFolderId);
}
