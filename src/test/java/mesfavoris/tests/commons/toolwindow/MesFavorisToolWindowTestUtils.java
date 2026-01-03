package mesfavoris.tests.commons.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.PlatformTestUtil;
import mesfavoris.internal.toolwindow.BookmarksTreeComponent;
import mesfavoris.internal.toolwindow.MesFavorisToolWindowFactory;
import mesfavoris.internal.toolwindow.MesFavorisToolWindowUtils;
import mesfavoris.model.BookmarkId;

import javax.swing.tree.TreePath;

/**
 * Utility class for testing MesFavoris tool window
 */
public class MesFavorisToolWindowTestUtils {

    /**
     * Setup the MesFavoris tool window for testing.
     *
     * @param project the project
     * @return the tool window
     */
    public static ToolWindow setupMesfavorisToolWindow(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("mesfavoris");
        if (toolWindow == null) {
            toolWindow = toolWindowManager.registerToolWindow("mesfavoris", true, ToolWindowAnchor.RIGHT);
        }
        MesFavorisToolWindowFactory factory = new MesFavorisToolWindowFactory();
        factory.createToolWindowContent(project, toolWindow);

        return toolWindow;
    }

    /**
     * Waits for the bookmark to be selected in the tree by dispatching all pending events
     *
     * @param project
     * @param bookmarkId the bookmark ID to wait for
     */
    public static void waitUntilBookmarkSelected(Project project, BookmarkId bookmarkId) {
        BookmarksTreeComponent tree = MesFavorisToolWindowUtils.findBookmarksTree(project);
        waitUntilBookmarkSelected(tree, bookmarkId);
    }

    /**
     * Waits for the bookmark to be selected in the tree by dispatching all pending events
     *
     * @param tree the bookmarks tree component
     * @param bookmarkId the bookmark ID to wait for
     */
    public static void waitUntilBookmarkSelected(BookmarksTreeComponent tree, BookmarkId bookmarkId) {
        // Dispatch all pending events to process the invokeLater and activate callbacks
        for (int i = 0; i < 10; i++) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            TreePath path = tree.getSelectionPath();
            if (path != null && tree.getBookmark(path).getId().equals(bookmarkId)) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }
}

