package mesfavoris.internal.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Utility methods for working with the MesFavoris tool window
 */
public class MesFavorisToolWindowUtils {

    @Nullable
    public static MesFavorisPanel findMesFavorisPanel(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("mesfavoris");
        if (toolWindow == null) {
            return null;
        }
        Content content = toolWindow.getContentManager().getContent(0);
        if (content != null && content.getComponent() instanceof MesFavorisPanel panel) {
            return panel;
        }
        return null;
    }

    @Nullable
    public static BookmarksTreeComponent findBookmarksTree(Project project) {
        MesFavorisPanel panel = findMesFavorisPanel(project);
        if (panel == null) {
            return null;
        }
        return findBookmarksTreeRecursive(panel);
    }

    @Nullable
    private static BookmarksTreeComponent findBookmarksTreeRecursive(Component component) {
        if (component instanceof BookmarksTreeComponent tree) {
            return tree;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                BookmarksTreeComponent tree = findBookmarksTreeRecursive(child);
                if (tree != null) {
                    return tree;
                }
            }
        }
        return null;
    }
}
