package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action to delete credentials for a specific remote bookmarks store.
 * Only enabled when the store is disconnected and has no mappings.
 */
public class DeleteCredentialsAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(DeleteCredentialsAction.class);

    private final String storeId;

    public DeleteCredentialsAction(@NotNull String storeId, @NotNull String label) {
        super();
        this.storeId = storeId;
        getTemplatePresentation().setText("Delete %s Credentials".formatted(label));
        getTemplatePresentation().setDescription("Delete stored credentials for %s".formatted(label));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            event.getPresentation().setVisible(true);
            event.getPresentation().setEnabled(false);
            return;
        }

        RemoteBookmarksStoreManager storeManager = project.getService(RemoteBookmarksStoreManager.class);
        IRemoteBookmarksStore store = storeManager.getRemoteBookmarksStore(storeId).orElse(null);

        if (store == null) {
            event.getPresentation().setVisible(false);
            event.getPresentation().setEnabled(false);
            return;
        }

        // Always visible
        event.getPresentation().setVisible(true);

        // Only enabled if disconnected and no mappings
        boolean isDisconnected = store.getState() == IRemoteBookmarksStore.State.disconnected;
        boolean hasNoMappings = store.getRemoteBookmarkFolders().isEmpty();
        event.getPresentation().setEnabled(isDisconnected && hasNoMappings);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        RemoteBookmarksStoreManager storeManager = project.getService(RemoteBookmarksStoreManager.class);
        IRemoteBookmarksStore store = storeManager.getRemoteBookmarksStore(storeId).orElse(null);

        if (store == null) {
            return;
        }

        String label = store.getDescriptor().label();

        // Confirm deletion
        int result = Messages.showYesNoDialog(
                project,
                "Are you sure you want to delete the credentials for %s?".formatted(label),
                "Delete Credentials",
                Messages.getQuestionIcon()
        );

        if (result != Messages.YES) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Modal(project, "Deleting Credentials", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Deleting credentials for %s...".formatted(label));
                try {
                    store.deleteCredentials();
                    showSuccessMessage(project, label);
                } catch (Exception e) {
                    LOG.error("Failed to delete credentials for %s".formatted(label), e);
                    showErrorMessage(project, label, e);
                }
            }
        });
    }

    private void showSuccessMessage(@NotNull Project project, @NotNull String label) {
        ApplicationManager.getApplication().invokeLater(() ->
                Messages.showInfoMessage(
                        project,
                        "Credentials for %s successfully deleted".formatted(label),
                        "Credentials Deleted"
                ));
    }

    private void showErrorMessage(@NotNull Project project, @NotNull String label, @NotNull Exception e) {
        ApplicationManager.getApplication().invokeLater(() ->
                Messages.showErrorDialog(
                        project,
                        "Failed to delete credentials for %s: %s".formatted(label, e.getMessage()),
                        "Error Deleting Credentials"
                ));
    }
}

