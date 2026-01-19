package mesfavoris.internal.markers.highlighters;

import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.util.messages.Topic;
import mesfavoris.model.BookmarkId;

import java.util.List;

public interface BookmarksHighlightersListener {
    Topic<BookmarksHighlightersListener> TOPIC = Topic.create("BookmarksHighlightersListener", BookmarksHighlightersListener.class);

    default List<BookmarkId> getBookmarkIds(RangeHighlighterEx highlighter) {
        return highlighter.getUserData(BookmarksHighlighters.BOOKMARK_IDS_KEY);
    }

    void bookmarkHighlighterDeleted(List<BookmarkId> bookmarkIds);

    void bookmarkHighlighterMoved(RangeHighlighterEx bookmarkHighlighter);
}
