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
import mesfavoris.internal.service.operations.AddToRemoteBookmarksStoreOperation;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Action to add a bookmark folder to a specific remote bookmarks store.
 * Only enabled when:
 * - A single folder is selected
 * - The store is connected
 * - The folder can be added to the store
 */
public class AddFolderToRemoteStoreAction extends AbstractBookmarkAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(AddFolderToRemoteStoreAction.class);
    
    private final IRemoteBookmarksStore store;

    public AddFolderToRemoteStoreAction(@NotNull IRemoteBookmarksStore store) {
        super();
        this.store = store;

        String label = store.getDescriptor().label();
        getTemplatePresentation().setText("Add to %s".formatted(label));
        getTemplatePresentation().setDescription("Add bookmark folder to %s".formatted(label));
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

        // Check if we can add this folder to the store
        AddToRemoteBookmarksStoreOperation operation = getOperation(event);
        boolean canAdd = operation.canAddToRemoteBookmarkStore(store.getDescriptor().id(), selectedFolder.getId());
        event.getPresentation().setEnabled(canAdd);
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

        BookmarksService bookmarksService = getBookmarksService(event);
        String storeId = store.getDescriptor().id();
        String label = store.getDescriptor().label();

        ProgressManager.getInstance().run(new Task.Modal(project, "Adding folder to %s".formatted(label), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Adding bookmark folder to %s...".formatted(label));
                try {
                    AddToRemoteBookmarksStoreOperation operation = getOperation(event);
                    operation.addToRemoteBookmarksStore(storeId, selectedFolder.getId(), indicator);
                    showSuccessMessage(project, label);
                } catch (BookmarksException e) {
                    LOG.error("Failed to add folder to %s".formatted(label), e);
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

    private AddToRemoteBookmarksStoreOperation getOperation(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        BookmarksService bookmarksService = getBookmarksService(event);
        RemoteBookmarksStoreManager remoteBookmarksStoreManager = project.getService(RemoteBookmarksStoreManager.class);
        return new AddToRemoteBookmarksStoreOperation(bookmarksService.getBookmarkDatabase(), remoteBookmarksStoreManager);
    }

    private void showSuccessMessage(@NotNull Project project, @NotNull String label) {
        ApplicationManager.getApplication().invokeLater(() ->
            Messages.showInfoMessage(
                project,
                "Bookmark folder successfully added to %s".formatted(label),
                "Folder Added"
            ));
    }

    private void showErrorMessage(@NotNull Project project, @NotNull String label, @NotNull BookmarksException e) {
        ApplicationManager.getApplication().invokeLater(() ->
            Messages.showErrorDialog(
                project,
                "Failed to add bookmark folder to %s: %s".formatted(label, e.getMessage()),
                "Error Adding Folder"
            ));
    }
}

