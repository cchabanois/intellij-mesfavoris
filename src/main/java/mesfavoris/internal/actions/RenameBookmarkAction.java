package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import mesfavoris.internal.toolwindow.BookmarksTreeComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreePath;
import java.awt.*;

public class RenameBookmarkAction extends AbstractBookmarkAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        BookmarksTreeComponent bookmarksTree = getBookmarksTree(e);
        if (bookmarksTree == null) {
            return;
        }
        TreePath selectedPath = bookmarksTree.getSelectionPath();
        e.getPresentation().setEnabledAndVisible(selectedPath != null && bookmarksTree.isPathEditable(selectedPath));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        BookmarksTreeComponent bookmarksTree = getBookmarksTree(e);
        if (bookmarksTree == null) {
            return;
        }
        TreePath selectedPath = bookmarksTree.getSelectionPath();
        if (selectedPath != null && bookmarksTree.isPathEditable(selectedPath)) {
            bookmarksTree.startEditingAtPath(selectedPath);
        }
    }

    private BookmarksTreeComponent getBookmarksTree(@NotNull AnActionEvent e) {
        Component component = e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT);
        return component instanceof BookmarksTreeComponent bookmarksTree ? bookmarksTree : null;
    }

}
