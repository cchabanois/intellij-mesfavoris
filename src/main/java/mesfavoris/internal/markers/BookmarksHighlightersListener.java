package mesfavoris.internal.markers;

import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.util.messages.Topic;
import mesfavoris.model.BookmarkId;

public interface BookmarksHighlightersListener {
    Topic<BookmarksHighlightersListener> TOPIC = Topic.create("BookmarksHighlightersListener", BookmarksHighlightersListener.class);

    default BookmarkId getBookmarkId(RangeHighlighterEx highlighter) {
        return highlighter.getUserData(BookmarksHighlighters.BOOKMARK_ID_KEY);
    }

    void bookmarkHighlighterDeleted(RangeHighlighterEx bookmarkHighlighter);

    void bookmarkHighlighterAdded(RangeHighlighterEx bookmarkHighlighter);

    void bookmarkHighlighterUpdated(RangeHighlighterEx bookmarkHighlighter);
}
