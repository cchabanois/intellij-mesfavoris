package mesfavoris.gdrive.actions;

import com.google.api.services.drive.model.File;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import mesfavoris.BookmarksException;
import mesfavoris.gdrive.BookmarksGDriveService;
import mesfavoris.gdrive.connection.GDriveConnectionManager;
import mesfavoris.gdrive.dialogs.ImportBookmarksFileDialog;
import mesfavoris.gdrive.mappings.BookmarkMappingsStore;
import mesfavoris.gdrive.operations.ImportBookmarkFileOperation;
import mesfavoris.internal.actions.AbstractBookmarkAction;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ImportBookmarksFromGDriveAction extends AbstractBookmarkAction {
    private static final Logger LOG = Logger.getInstance(ImportBookmarksFromGDriveAction.class);

    public ImportBookmarksFromGDriveAction() {
        super();
        getTemplatePresentation().setText("Import bookmarks...");
        getTemplatePresentation().setDescription("Import bookmark files from Google Drive");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        super.update(event);
        Project project = event.getProject();
        if (project == null) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }
        BookmarksGDriveService gdriveService = project.getService(BookmarksGDriveService.class);
        GDriveConnectionManager connectionManager = gdriveService.getConnectionManager();
        event.getPresentation().setEnabledAndVisible(connectionManager.getState() == IRemoteBookmarksStore.State.connected);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        BookmarksGDriveService gdriveService = project.getService(BookmarksGDriveService.class);
        GDriveConnectionManager connectionManager = gdriveService.getConnectionManager();
        if (connectionManager.getState() != IRemoteBookmarksStore.State.connected) {
            Messages.showErrorDialog(project, "Not connected to Google Drive", "Error");
            return;
        }

        BookmarksService bookmarksService = project.getService(BookmarksService.class);
        BookmarkMappingsStore bookmarkMappingsStore = project.getService(BookmarkMappingsStore.class);

        List<Bookmark> selectedBookmarks = getSelectedBookmarks(event);
        BookmarkFolder targetFolder = selectedBookmarks.isEmpty() || !(selectedBookmarks.get(0) instanceof BookmarkFolder)
                ? bookmarksService.getBookmarksTree().getRootFolder()
                : (BookmarkFolder) selectedBookmarks.get(0);

        ImportBookmarksFileDialog dialog = new ImportBookmarksFileDialog(
                project,
                connectionManager.getDrive(),
                connectionManager.getApplicationFolderId(),
                bookmarkMappingsStore);

        if (!dialog.showAndGet()) {
            return;
        }

        List<File> files = dialog.getSelectedFiles();
        if (files.isEmpty()) {
            return;
        }

        importBookmarkFiles(project, connectionManager, bookmarkMappingsStore, bookmarksService, targetFolder, files);
    }

    private void importBookmarkFiles(@NotNull Project project,
                                     @NotNull GDriveConnectionManager connectionManager,
                                     @NotNull BookmarkMappingsStore bookmarkMappingsStore,
                                     @NotNull BookmarksService bookmarksService,
                                     @NotNull BookmarkFolder targetFolder,
                                     @NotNull List<File> files) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Importing Bookmarks Files", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                ImportBookmarkFileOperation importOperation = new ImportBookmarkFileOperation(
                        connectionManager.getDrive(),
                        bookmarkMappingsStore,
                        bookmarksService,
                        Optional.of(connectionManager.getApplicationFolderId()));

                for (int i = 0; i < files.size(); i++) {
                    if (indicator.isCanceled()) {
                        break;
                    }

                    File file = files.get(i);
                    indicator.setFraction((double) i / files.size());
                    indicator.setText("Importing %s...".formatted(file.getTitle()));

                    try {
                        importOperation.importBookmarkFile(targetFolder.getId(), file.getId(), indicator);
                    } catch (BookmarksException | IOException e) {
                        LOG.error("Could not import bookmark file: %s".formatted(file.getTitle()), e);
                        String errorMessage = "Could not import bookmark file '%s': %s".formatted(file.getTitle(), e.getMessage());
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showErrorDialog(project, errorMessage, "Import Error"));
                    }
                }

                indicator.setFraction(1.0);
            }
        });
    }

}
