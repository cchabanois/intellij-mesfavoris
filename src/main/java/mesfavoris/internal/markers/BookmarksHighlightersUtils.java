package mesfavoris.internal.markers;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import mesfavoris.model.BookmarkId;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BookmarksHighlightersUtils {

    /**
     * Find all bookmarks at the specified line number.
     * Multiple bookmarks can exist on the same line.
     */
    public static List<BookmarkId> findBookmarksAtLine(Project project, Document document, int lineNumber) {
        List<BookmarkId> bookmarkIds = new ArrayList<>();
        List<RangeHighlighterEx> highlighters = BookmarksHighlighters.getBookmarksHighlighters(project, document);

        for (RangeHighlighterEx highlighter : highlighters) {
            int highlighterLine = document.getLineNumber(highlighter.getStartOffset());
            if (highlighterLine == lineNumber) {
                BookmarkId bookmarkId = highlighter.getUserData(BookmarksHighlighters.BOOKMARK_ID_KEY);
                if (bookmarkId != null) {
                    bookmarkIds.add(bookmarkId);
                }
            }
        }

        return bookmarkIds;
    }

    /**
     * Get all bookmarks at the current caret position.
     * Multiple bookmarks can exist on the same line.
     */
    public static List<BookmarkId> getBookmarksAtCaret(@NotNull Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return List.of();
        }

        Document document = editor.getDocument();
        int caretOffset = editor.getCaretModel().getOffset();
        int lineNumber = document.getLineNumber(caretOffset);

        return findBookmarksAtLine(project, document, lineNumber);
    }

}
