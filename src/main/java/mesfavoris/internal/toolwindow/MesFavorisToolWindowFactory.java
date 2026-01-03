package mesfavoris.internal.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.actions.CollapseAllAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import mesfavoris.icons.MesFavorisIcons;
import mesfavoris.internal.actions.ConnectToRemoteBookmarksStoreAction;
import mesfavoris.internal.actions.SettingsActionGroup;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarksStoreExtensionManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MesFavorisToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        final ToolWindowEx toolWindowEx = (ToolWindowEx) toolWindow;
        toolWindowEx.setIcon(MesFavorisIcons.toolWindowMesFavoris);
        toolWindowEx.setTitleActions(getTitleActions(project));

        MesFavorisPanel panel = new MesFavorisPanel(project);

        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(panel, null, false);
        contentManager.addContent(content);
        DataManager.registerDataProvider(panel, panel);
    }

    private List<AnAction> getTitleActions(@NotNull Project project) {
        List<AnAction> actions = new ArrayList<>();

        actions.add(createCollapseAllAction());
        actions.addAll(createRemoteBookmarksStoreActions(project));
        actions.add(createSettingsActionGroup());

        return actions;
    }

    private AnAction createCollapseAllAction() {
        final CollapseAllAction collapseAction = new CollapseAllAction();
        collapseAction.getTemplatePresentation().setIcon(AllIcons.Actions.Collapseall);
        return collapseAction;
    }

    private List<AnAction> createRemoteBookmarksStoreActions(@NotNull Project project) {
        RemoteBookmarksStoreExtensionManager manager = project.getService(RemoteBookmarksStoreExtensionManager.class);
        List<IRemoteBookmarksStore> stores = manager.getStores();

        return stores.stream()
                .map(ConnectToRemoteBookmarksStoreAction::new)
                .collect(java.util.stream.Collectors.toList());
    }

    private AnAction createSettingsActionGroup() {
        final SettingsActionGroup settingsActionGroup = new SettingsActionGroup();
        settingsActionGroup.getTemplatePresentation().setIcon(AllIcons.General.Settings);
        return settingsActionGroup;
    }
}
