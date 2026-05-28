package mesfavoris.github.changes;

import com.intellij.util.messages.Topic;
import mesfavoris.model.BookmarkId;

/** MessageBus topic fired when a remote Gist has been modified since the last poll. */
public interface IGistChangeListener {
    Topic<IGistChangeListener> TOPIC =
            Topic.create("GistChangeListener", IGistChangeListener.class);

    void gistChanged(BookmarkId bookmarkFolderId, String gistId);
}
