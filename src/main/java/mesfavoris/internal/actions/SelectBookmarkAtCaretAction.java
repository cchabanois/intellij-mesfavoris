package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import mesfavoris.internal.markers.highlighters.BookmarksHighlightersUtils;
import mesfavoris.internal.toolwindow.BookmarksTreeComponent;
import mesfavoris.internal.toolwindow.MesFavorisToolWindowUtils;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreePath;
import java.util.List;

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

        // Find all bookmarks at current caret position
        List<BookmarkId> bookmarkIds = BookmarksHighlightersUtils.getBookmarksAtCaret(project);
        if (bookmarkIds.isEmpty()) {
            return;
        }

        // Select the first bookmark in the tree (this also activates the tool window and sets focus on the tree)
        IBookmarksService bookmarksService = project.getService(IBookmarksService.class);
        bookmarksService.selectBookmarkInTree(bookmarkIds.get(0));
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }

        List<BookmarkId> bookmarkIds = BookmarksHighlightersUtils.getBookmarksAtCaret(project);
        if (bookmarkIds.isEmpty()) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }

        // Always visible if there's a bookmark at caret
        event.getPresentation().setVisible(true);

        // Enabled only if the first bookmark at caret is not already selected in the tree
        boolean isAlreadySelected = isBookmarkSelectedInTree(project, bookmarkIds.get(0));
        event.getPresentation().setEnabled(!isAlreadySelected);
    }

    private boolean isBookmarkSelectedInTree(Project project, BookmarkId bookmarkId) {
        BookmarksTreeComponent tree = MesFavorisToolWindowUtils.findBookmarksTree(project);
        if (tree == null) {
            return false;
        }

        TreePath selectionPath = tree.getSelectionPath();
        if (selectionPath == null) {
            return false;
        }

        Bookmark selectedBookmark = tree.getBookmark(selectionPath);
        return selectedBookmark != null && selectedBookmark.getId().equals(bookmarkId);
    }
}

