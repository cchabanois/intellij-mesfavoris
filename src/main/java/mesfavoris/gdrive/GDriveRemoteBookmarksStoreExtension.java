package mesfavoris.gdrive;

import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import mesfavoris.gdrive.changes.BookmarksFileChangeManager;
import mesfavoris.gdrive.connection.GDriveConnectionManager;
import mesfavoris.gdrive.mappings.BookmarkMappingsStore;
import mesfavoris.remote.AbstractRemoteBookmarksStore;
import mesfavoris.remote.AbstractRemoteBookmarksStoreExtension;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Extension for Google Drive remote bookmarks storage.
 * Provides integration with Google Drive to store and synchronize bookmarks.
 */
public class GDriveRemoteBookmarksStoreExtension extends AbstractRemoteBookmarksStoreExtension {

    private static final String ID = "gdrive";
    private static final String LABEL = "Google Drive";
    private static final String APPLICATION_NAME = "Mes Favoris";
    private static final String APPLICATION_FOLDER_NAME = "mesfavoris";

    public GDriveRemoteBookmarksStoreExtension() {
        super(ID, LABEL, GDriveIcons.GDRIVE, GDriveIcons.GDRIVE_OVERLAY);
    }

    @NotNull
    @Override
    public AbstractRemoteBookmarksStore createStore(@NotNull Project project) {
        // Get or create the connection manager
        GDriveConnectionManager connectionManager = createConnectionManager(project);
        
        // Get the bookmark mappings store (project-level service)
        BookmarkMappingsStore mappingsStore = project.getService(BookmarkMappingsStore.class);
        
        // Get the scheduled executor service
        ScheduledExecutorService scheduledExecutorService = AppExecutorUtil.getAppScheduledExecutorService();
        
        // Create the bookmarks file change manager
        BookmarksFileChangeManager changeManager = new BookmarksFileChangeManager(
                connectionManager, 
                mappingsStore, 
                scheduledExecutorService);
        
        // Create and return the store
        return new GDriveRemoteBookmarksStore(project, connectionManager, mappingsStore, changeManager);
    }

    private GDriveConnectionManager createConnectionManager(@NotNull Project project) {
        GDriveConnectionManager connectionManager = new GDriveConnectionManager(
                project, 
                APPLICATION_NAME, 
                APPLICATION_FOLDER_NAME);
        
        try {
            connectionManager.init();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to initialize Google Drive connection manager", e);
        }
        
        return connectionManager;
    }
}

