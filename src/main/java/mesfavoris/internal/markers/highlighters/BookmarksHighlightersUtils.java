package mesfavoris.internal.markers.highlighters;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import mesfavoris.markers.IBookmarksHighlighters;
import mesfavoris.model.BookmarkId;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BookmarksHighlightersUtils {

    /**
     * Find all bookmarks at the specified line number.
     * Multiple bookmarks can exist on the same line.
     */
    public static List<BookmarkId> findBookmarksAtLine(Project project, Document document, int lineNumber) {
        IBookmarksHighlighters bookmarksHighlighters = project.getService(IBookmarksHighlighters.class);
        if (bookmarksHighlighters == null) {
            return List.of();
        }

        RangeHighlighterEx highlighter = bookmarksHighlighters.findBookmarkHighlighterAtLine(document, lineNumber);
        if (highlighter == null) {
            return List.of();
        }

        List<BookmarkId> ids = highlighter.getUserData(BookmarksHighlighters.BOOKMARK_IDS_KEY);
        return ids != null ? ids : List.of();
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
