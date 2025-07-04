package mesfavoris.internal.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.treeStructure.actions.CollapseAllAction;
import mesfavoris.icons.MesFavorisIcons;
import mesfavoris.internal.actions.ManagePlaceholdersAction;
import org.jetbrains.annotations.NotNull;

public class MesFavorisToolWindowFactory  implements ToolWindowFactory {

    // Create the tool window content.
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        final ToolWindowEx toolWindowEx = (ToolWindowEx) toolWindow;
        toolWindowEx.setIcon(MesFavorisIcons.toolWindowMesFavoris);

        // Add actions to the tool window
        final CollapseAllAction collapseAction = new CollapseAllAction(null);
        collapseAction.getTemplatePresentation().setIcon(AllIcons.Actions.Collapseall);

        final ManagePlaceholdersAction managePlaceholdersAction = new ManagePlaceholdersAction();
        managePlaceholdersAction.getTemplatePresentation().setIcon(AllIcons.Actions.Properties);

        toolWindowEx.setTitleActions(collapseAction, managePlaceholdersAction);

        MesFavorisPanel panel = new MesFavorisPanel(project);

        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(panel, null, false);
        contentManager.addContent(content);
        DataManager.registerDataProvider(panel, panel);
    }
}
