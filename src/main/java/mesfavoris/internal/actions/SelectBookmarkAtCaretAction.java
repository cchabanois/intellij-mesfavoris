package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import mesfavoris.internal.markers.BookmarksHighlighters;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Action to select the bookmark corresponding to the current caret position in the editor
 */
public class SelectBookmarkAtCaretAction extends AnAction implements DumbAware {

    public SelectBookmarkAtCaretAction() {
        super("Select Bookmark at Caret", "Select the bookmark corresponding to the current caret position", null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return;
        }

        Document document = editor.getDocument();
        int caretOffset = editor.getCaretModel().getOffset();
        int lineNumber = document.getLineNumber(caretOffset);

        // Find bookmark highlighter at current line
        Optional<BookmarkId> bookmarkId = findBookmarkAtLine(project, document, lineNumber);
        if (bookmarkId.isEmpty()) {
            return;
        }

        // Select the bookmark in the tree using the operation
        IBookmarksService bookmarksService = project.getService(IBookmarksService.class);
        bookmarksService.selectBookmarkInTree(bookmarkId.get());
    }

    private Optional<BookmarkId> findBookmarkAtLine(Project project, Document document, int lineNumber) {
        List<RangeHighlighterEx> highlighters = BookmarksHighlighters.getBookmarksHighlighters(project, document);

        for (RangeHighlighterEx highlighter : highlighters) {
            int highlighterLine = document.getLineNumber(highlighter.getStartOffset());
            if (highlighterLine == lineNumber) {
                BookmarkId bookmarkId = highlighter.getUserData(BookmarksHighlighters.BOOKMARK_ID_KEY);
                if (bookmarkId != null) {
                    return Optional.of(bookmarkId);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }

        // Check if there's an active editor with a bookmark at the current line
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }

        Document document = editor.getDocument();
        int caretOffset = editor.getCaretModel().getOffset();
        int lineNumber = document.getLineNumber(caretOffset);

        Optional<BookmarkId> bookmarkId = findBookmarkAtLine(project, document, lineNumber);
        event.getPresentation().setEnabledAndVisible(bookmarkId.isPresent());
    }
}

