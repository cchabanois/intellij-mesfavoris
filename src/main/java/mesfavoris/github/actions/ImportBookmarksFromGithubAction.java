package mesfavoris.github.actions;

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
import mesfavoris.github.BookmarksGithubService;
import mesfavoris.github.connection.GithubConnectionManager;
import mesfavoris.github.dialogs.ImportGistDialog;
import mesfavoris.github.mappings.GistMappingsStore;
import mesfavoris.github.operations.GistApiClient;
import mesfavoris.github.operations.ImportGistOperation;
import mesfavoris.internal.actions.AbstractBookmarkAction;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class ImportBookmarksFromGithubAction extends AbstractBookmarkAction {
    private static final Logger LOG = Logger.getInstance(ImportBookmarksFromGithubAction.class);

    public ImportBookmarksFromGithubAction() {
        super();
        getTemplatePresentation().setText("Import bookmarks...");
        getTemplatePresentation().setDescription("Import bookmark folders from GitHub Gists");
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
        GithubConnectionManager connectionManager = project.getService(BookmarksGithubService.class)
                .getConnectionManager();
        event.getPresentation().setEnabledAndVisible(
                connectionManager.getState() == IRemoteBookmarksStore.State.connected);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;

        GithubConnectionManager connectionManager = project.getService(BookmarksGithubService.class)
                .getConnectionManager();
        if (connectionManager.getState() != IRemoteBookmarksStore.State.connected) {
            Messages.showErrorDialog(project, "Not connected to GitHub", "Error");
            return;
        }

        IBookmarksService bookmarksService = project.getService(IBookmarksService.class);
        GistMappingsStore gistMappingsStore = project.getService(GistMappingsStore.class);
        GistApiClient apiClient = connectionManager.getGistApiClient();

        List<Bookmark> selectedBookmarks = getSelectedBookmarks(event);
        BookmarkId targetFolderId = selectedBookmarks.isEmpty() || !(selectedBookmarks.getFirst() instanceof BookmarkFolder)
                ? bookmarksService.getBookmarksTree().getRootFolder().getId()
                : selectedBookmarks.getFirst().getId();

        ImportGistDialog dialog = new ImportGistDialog(project, apiClient, gistMappingsStore);
        if (!dialog.showAndGet()) return;

        List<GistApiClient.GistResponse> gists = dialog.getSelectedGists();
        if (gists.isEmpty()) return;

        importGists(project, apiClient, gistMappingsStore, bookmarksService, targetFolderId, gists);
    }

    private void importGists(@NotNull Project project, @NotNull GistApiClient apiClient,
                             @NotNull GistMappingsStore gistMappingsStore,
                             @NotNull IBookmarksService bookmarksService,
                             @NotNull BookmarkId targetFolderId,
                             @NotNull List<GistApiClient.GistResponse> gists) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Importing Bookmarks", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                ImportGistOperation op = new ImportGistOperation(apiClient, gistMappingsStore, bookmarksService);
                for (int i = 0; i < gists.size(); i++) {
                    if (indicator.isCanceled()) break;
                    GistApiClient.GistResponse gist = gists.get(i);
                    indicator.setFraction((double) i / gists.size());
                    indicator.setText("Importing %s...".formatted(
                            gist.description != null ? gist.description : gist.id));
                    try {
                        op.importGist(targetFolderId, gist.id, indicator);
                    } catch (IOException | BookmarksException e) {
                        LOG.error("Could not import gist: " + gist.id, e);
                        String msg = "Could not import gist '%s': %s".formatted(gist.id, e.getMessage());
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showErrorDialog(project, msg, "Import Error"));
                    }
                }
                indicator.setFraction(1.0);
            }
        });
    }
}
