package mesfavoris.internal.service.operations;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import mesfavoris.internal.toolwindow.BookmarksTreeComponent;
import mesfavoris.internal.toolwindow.MesFavorisToolWindowUtils;
import mesfavoris.model.BookmarkId;

import javax.swing.tree.TreePath;
import java.util.Optional;

/**
 * Operation to select a bookmark in the bookmarks tree view
 */
public class SelectBookmarkInTreeOperation {
    private final Project project;

    public SelectBookmarkInTreeOperation(Project project) {
        this.project = project;
    }

    /**
     * Selects the specified bookmark in the tree and activates the tool window
     *
     * @param bookmarkId the bookmark to select
     */
    public void selectBookmark(BookmarkId bookmarkId) {
        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("mesfavoris");
            if (toolWindow == null) {
                return;
            }

            // Activate the tool window
            toolWindow.activate(() -> {
                BookmarksTreeComponent tree = MesFavorisToolWindowUtils.findBookmarksTree(project);
                if (tree != null) {
                    Optional<TreePath> treePath = tree.getTreePathForBookmark(bookmarkId);
                    treePath.ifPresent(path -> {
                        tree.setSelectionPath(path);
                        tree.scrollPathToVisible(path);
                    });
                }
            });
        });
    }
}

