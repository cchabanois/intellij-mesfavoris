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

    /**
     * Finds the BookmarksTreeComponent in the MesFavoris tool window
     *
     * @param project the project
     * @return the BookmarksTreeComponent, or null if not found
     */
    @Nullable
    public static BookmarksTreeComponent findBookmarksTree(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("mesfavoris");
        if (toolWindow == null) {
            return null;
        }

        Content content = toolWindow.getContentManager().getContent(0);
        if (content != null && content.getComponent() instanceof MesFavorisPanel panel) {
            return findBookmarksTreeInPanel(panel);
        }
        return null;
    }

    /**
     * Finds the BookmarksTreeComponent in a MesFavorisPanel
     *
     * @param panel the panel to search in
     * @return the BookmarksTreeComponent, or null if not found
     */
    @Nullable
    private static BookmarksTreeComponent findBookmarksTreeInPanel(MesFavorisPanel panel) {
        for (Component component : panel.getComponents()) {
            BookmarksTreeComponent tree = findBookmarksTreeRecursive(component);
            if (tree != null) {
                return tree;
            }
        }
        return null;
    }

    /**
     * Recursively searches for a BookmarksTreeComponent in a component hierarchy
     *
     * @param component the component to search in
     * @return the BookmarksTreeComponent, or null if not found
     */
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

