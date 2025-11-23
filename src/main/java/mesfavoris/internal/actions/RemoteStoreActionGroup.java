package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import mesfavoris.remote.IRemoteBookmarksStore;
import org.jetbrains.annotations.NotNull;

/**
 * Action group for a specific remote bookmarks store.
 * Contains actions like "Add to [Store]", "Remove from [Store]", etc.
 */
public class RemoteStoreActionGroup extends DefaultActionGroup implements DumbAware {
    private final IRemoteBookmarksStore store;
    private final AddFolderToRemoteStoreAction addAction;
    private final RemoveFromRemoteBookmarksStoreAction removeAction;

    public RemoteStoreActionGroup(@NotNull IRemoteBookmarksStore store) {
        super(store.getDescriptor().label(), true);
        this.store = store;
        this.addAction = new AddFolderToRemoteStoreAction(store);
        this.removeAction = new RemoveFromRemoteBookmarksStoreAction(store);
        getTemplatePresentation().setIcon(store.getDescriptor().icon());
        getTemplatePresentation().setPopupGroup(true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public AnAction @NotNull [] getChildren(@NotNull AnActionEvent event) {
        return new AnAction[] { addAction, removeAction };
    }
}

