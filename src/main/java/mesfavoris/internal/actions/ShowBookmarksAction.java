package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import mesfavoris.internal.markers.highlighters.BookmarksHighlightersUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Composite action that shows bookmarks with intelligent behavior:
 * - If cursor is on a line with a bookmark, selects that bookmark in the tree
 * - Otherwise, shows/activates the Mes Favoris tool window
 */
public class ShowBookmarksAction extends AnAction implements DumbAware {
    public static final String ACTION_ID = "mesfavoris.actions.ShowBookmarksAction";
    private final SelectBookmarkAtCaretAction selectBookmarkAction;
    private final ShowMesFavorisToolWindowAction showToolWindowAction;

    public ShowBookmarksAction() {
        super("Show Bookmarks", "Select bookmark at caret or show Mes Favoris tool window", null);
        this.selectBookmarkAction = new SelectBookmarkAtCaretAction();
        this.showToolWindowAction = new ShowMesFavorisToolWindowAction();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // Always enabled when we have a project
        event.getPresentation().setEnabledAndVisible(event.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // Try to select bookmark at caret first
        if (event.getProject() != null && !BookmarksHighlightersUtils.getBookmarksAtCaret(event.getProject()).isEmpty()) {
            selectBookmarkAction.actionPerformed(event);
        } else {
            // Fall back to showing the tool window
            showToolWindowAction.actionPerformed(event);
        }
    }
}

