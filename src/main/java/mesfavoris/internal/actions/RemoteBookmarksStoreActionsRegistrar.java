package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarksStoreExtensionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Registers individual actions for each remote bookmarks store at project startup.
 * This makes the actions available in the "Find Action" dialog (Ctrl+Shift+A).
 */
public class RemoteBookmarksStoreActionsRegistrar implements ProjectActivity {
    private static final Logger LOG = Logger.getInstance(RemoteBookmarksStoreActionsRegistrar.class);
    private static final String ACTION_ID_PREFIX = "mesfavoris.actions.ConnectToRemoteBookmarksStore.";

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        registerActions(project);
        return null;
    }

    private void registerActions(Project project) {
        RemoteBookmarksStoreExtensionManager manager = project.getService(RemoteBookmarksStoreExtensionManager.class);
        List<IRemoteBookmarksStore> stores = manager.getStores();
        ActionManager actionManager = ActionManager.getInstance();

        for (IRemoteBookmarksStore store : stores) {
            String actionId = ACTION_ID_PREFIX + store.getDescriptor().id();
            AnAction action = new ConnectToRemoteBookmarksStoreAction(store);
            actionManager.registerAction(actionId, action);
            LOG.info("Registered action: " + actionId);
        }
    }
}

