package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Action group for settings menu
 */
public class SettingsActionGroup extends DefaultActionGroup implements DumbAware {
    private final SettingsAction settingsAction;
    private final ManagePlaceholdersAction managePlaceholdersAction;
    private final SwapBookmarkShortcutsAction swapShortcutsAction;
    private final ToggleBookmarkCommentInlayHintsAction toggleInlayHintsAction;

    public SettingsActionGroup() {
        super("Settings", true);
        this.settingsAction = new SettingsAction();
        this.managePlaceholdersAction = new ManagePlaceholdersAction();
        this.swapShortcutsAction = new SwapBookmarkShortcutsAction();
        this.toggleInlayHintsAction = new ToggleBookmarkCommentInlayHintsAction();
        getTemplatePresentation().setPopupGroup(true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public AnAction @NotNull [] getChildren(@NotNull AnActionEvent event) {
        List<AnAction> actions = new ArrayList<>();
        actions.add(settingsAction);
        actions.add(managePlaceholdersAction);
        actions.add(swapShortcutsAction);
        actions.add(new Separator());
        actions.add(toggleInlayHintsAction);
        actions.add(new Separator());

        // Add delete credentials actions for each remote store
        Project project = event.getProject();
        if (project != null) {
            RemoteBookmarksStoreManager storeManager = project.getService(RemoteBookmarksStoreManager.class);
            for (IRemoteBookmarksStore store : storeManager.getRemoteBookmarksStores()) {
                actions.add(new DeleteCredentialsAction(store.getDescriptor().id(), store.getDescriptor().label()));
            }
        }

        return actions.toArray(new AnAction[0]);
    }
}

