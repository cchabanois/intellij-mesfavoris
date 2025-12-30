package mesfavoris.gdrive;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import mesfavoris.gdrive.actions.ImportBookmarksFromGDriveAction;
import mesfavoris.gdrive.actions.ViewInGDriveAction;
import mesfavoris.gdrive.changes.BookmarksFileChangeManager;
import mesfavoris.gdrive.connection.GDriveConnectionManager;
import mesfavoris.gdrive.mappings.BookmarkMappingsStore;
import mesfavoris.remote.AbstractRemoteBookmarksStore;
import mesfavoris.remote.AbstractRemoteBookmarksStoreExtension;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Extension for Google Drive remote bookmarks storage.
 * Provides integration with Google Drive to store and synchronize bookmarks.
 */
public class GDriveRemoteBookmarksStoreExtension extends AbstractRemoteBookmarksStoreExtension {

    private static final String ID = "gdrive";
    private static final String LABEL = "Google Drive";

    public GDriveRemoteBookmarksStoreExtension() {
        super(ID, LABEL, GDriveIcons.GDRIVE, GDriveIcons.GDRIVE_OVERLAY);
    }

    @NotNull
    @Override
    public AbstractRemoteBookmarksStore createStore(@NotNull Project project) {
        // Get the Google Drive service
        BookmarksGDriveService gdriveService = project.getService(BookmarksGDriveService.class);
        GDriveConnectionManager connectionManager = gdriveService.getConnectionManager();

        // Get the bookmark mappings store (project-level service)
        BookmarkMappingsStore mappingsStore = project.getService(BookmarkMappingsStore.class);

        // Get the scheduled executor service
        ScheduledExecutorService scheduledExecutorService = AppExecutorUtil.getAppScheduledExecutorService();

        // Create the bookmarks file change manager
        BookmarksFileChangeManager changeManager = new BookmarksFileChangeManager(
                project,
                connectionManager,
                mappingsStore,
                scheduledExecutorService);

        // Create and return the store
        return new GDriveRemoteBookmarksStore(project, connectionManager, mappingsStore, changeManager);
    }

    @NotNull
    @Override
    public List<AnAction> getAdditionalActions() {
        return List.of(
                new ImportBookmarksFromGDriveAction(),
                new ViewInGDriveAction()
        );
    }
}

