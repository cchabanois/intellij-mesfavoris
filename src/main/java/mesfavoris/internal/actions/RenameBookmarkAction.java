package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import mesfavoris.internal.toolwindow.BookmarksJTree;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreePath;
import java.awt.*;

public class RenameBookmarkAction extends AbstractBookmarkAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        BookmarksJTree bookmarksTree = getBookmarksTree(e);
        if (bookmarksTree == null) {
            return;
        }
        TreePath selectedPath = bookmarksTree.getSelectionPath();
        e.getPresentation().setEnabledAndVisible(selectedPath != null && bookmarksTree.isPathEditable(selectedPath));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        BookmarksJTree bookmarksTree = getBookmarksTree(e);
        if (bookmarksTree == null) {
            return;
        }
        TreePath selectedPath = bookmarksTree.getSelectionPath();
        if (selectedPath != null && bookmarksTree.isPathEditable(selectedPath)) {
            bookmarksTree.startEditingAtPath(selectedPath);
        }
    }

    private BookmarksJTree getBookmarksTree(@NotNull AnActionEvent e) {
        Component component = e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT);
        return component instanceof BookmarksJTree bookmarksTree ? bookmarksTree : null;
    }

}
