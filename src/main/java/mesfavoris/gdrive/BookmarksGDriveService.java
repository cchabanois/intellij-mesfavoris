package mesfavoris.gdrive;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import mesfavoris.gdrive.connection.GDriveConnectionManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Project-level service that provides access to Google Drive connection manager
 */
@Service(Service.Level.PROJECT)
public final class BookmarksGDriveService {
    private static final String APPLICATION_NAME = "Mes Favoris";
    private static final String APPLICATION_FOLDER_NAME = "mesfavoris";

    private final Project project;
    private GDriveConnectionManager connectionManager;

    public BookmarksGDriveService(@NotNull Project project) {
        this.project = project;
    }

    /**
     * Get the Google Drive connection manager, creating it if necessary
     */
    @NotNull
    public synchronized GDriveConnectionManager getConnectionManager() {
        if (connectionManager == null) {
            connectionManager = createConnectionManager();
        }
        return connectionManager;
    }

    private GDriveConnectionManager createConnectionManager() {
        GDriveConnectionManager manager = new GDriveConnectionManager(
                project,
                APPLICATION_NAME,
                APPLICATION_FOLDER_NAME);

        try {
            manager.init();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to initialize Google Drive connection manager", e);
        }

        return manager;
    }
}
