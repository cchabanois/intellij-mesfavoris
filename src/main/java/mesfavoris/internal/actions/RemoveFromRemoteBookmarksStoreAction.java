package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import mesfavoris.BookmarksException;
import mesfavoris.internal.service.operations.RemoveFromRemoteBookmarksStoreOperation;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Action to remove a bookmark folder from a specific remote bookmarks store.
 * Only enabled when:
 * - A single folder is selected
 * - The store is connected
 * - The folder is in the remote store
 */
public class RemoveFromRemoteBookmarksStoreAction extends AbstractBookmarkAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(RemoveFromRemoteBookmarksStoreAction.class);

    private final IRemoteBookmarksStore store;

    public RemoveFromRemoteBookmarksStoreAction(@NotNull IRemoteBookmarksStore store) {
        super();
        this.store = store;

        String label = store.getDescriptor().label();
        getTemplatePresentation().setText("Remove from %s".formatted(label));
        getTemplatePresentation().setDescription("Remove bookmark folder from %s".formatted(label));
        getTemplatePresentation().setIcon(store.getDescriptor().icon());
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

        // Always visible
        event.getPresentation().setVisible(true);

        BookmarkFolder selectedFolder = getSelectedBookmarkFolder(event);
        if (selectedFolder == null) {
            event.getPresentation().setEnabled(false);
            return;
        }

        // Check if we can remove this folder from the store
        RemoveFromRemoteBookmarksStoreOperation operation = getOperation(event);
        boolean canRemove = operation.canRemoveFromRemoteBookmarkStore(store.getDescriptor().id(), selectedFolder.getId());
        event.getPresentation().setEnabled(canRemove);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        BookmarkFolder selectedFolder = getSelectedBookmarkFolder(event);
        if (selectedFolder == null) {
            return;
        }

        String storeId = store.getDescriptor().id();
        String label = store.getDescriptor().label();

        ProgressManager.getInstance().run(new Task.Modal(project, "Removing folder from %s".formatted(label), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Removing bookmark folder from %s...".formatted(label));
                try {
                    RemoveFromRemoteBookmarksStoreOperation operation = getOperation(event);
                    operation.removeFromRemoteBookmarksStore(storeId, selectedFolder.getId(), indicator);
                    showSuccessMessage(project, label);
                } catch (BookmarksException e) {
                    LOG.error("Failed to remove folder from %s".formatted(label), e);
                    showErrorMessage(project, label, e);
                }
            }
        });
    }

    private BookmarkFolder getSelectedBookmarkFolder(@NotNull AnActionEvent event) {
        List<Bookmark> selectedBookmarks = getSelectedBookmarks(event);
        if (selectedBookmarks.size() != 1) {
            return null;
        }

        Bookmark bookmark = selectedBookmarks.get(0);
        if (!(bookmark instanceof BookmarkFolder)) {
            return null;
        }

        return (BookmarkFolder) bookmark;
    }

    private RemoveFromRemoteBookmarksStoreOperation getOperation(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        BookmarksService bookmarksService = getBookmarksService(event);
        RemoteBookmarksStoreManager remoteBookmarksStoreManager = project.getService(RemoteBookmarksStoreManager.class);
        return new RemoveFromRemoteBookmarksStoreOperation(bookmarksService.getBookmarkDatabase(), remoteBookmarksStoreManager);
    }

    private void showSuccessMessage(@NotNull Project project, @NotNull String label) {
        ApplicationManager.getApplication().invokeLater(() ->
            Messages.showInfoMessage(
                project,
                "Bookmark folder successfully removed from %s".formatted(label),
                "Folder Removed"
            ));
    }

    private void showErrorMessage(@NotNull Project project, @NotNull String label, @NotNull BookmarksException e) {
        ApplicationManager.getApplication().invokeLater(() ->
            Messages.showErrorDialog(
                project,
                "Failed to remove bookmark folder from %s: %s".formatted(label, e.getMessage()),
                "Error Removing Folder"
            ));
    }
}
