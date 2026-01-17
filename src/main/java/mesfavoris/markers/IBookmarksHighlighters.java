package mesfavoris.markers;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.util.Key;
import mesfavoris.model.BookmarkId;

import java.util.List;

public interface IBookmarksHighlighters {
    Key<List<BookmarkId>> BOOKMARK_IDS_KEY = new Key<>("bookmarkIds");

    List<RangeHighlighterEx> getBookmarksHighlighters(Document document);

    RangeHighlighterEx findBookmarkHighlighterAtLine(Document document, int lineNumber);
}
