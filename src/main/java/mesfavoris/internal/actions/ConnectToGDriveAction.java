package mesfavoris.internal.actions;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import mesfavoris.gdrive.connection.GDriveConnectionManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

import static com.google.api.client.auth.oauth2.StoredCredential.DEFAULT_DATA_STORE_ID;

/**
 * Action to connect to Google Drive and display tokens for testing purposes
 */
public class ConnectToGDriveAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ConnectToGDriveAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ProgressManager.getInstance().run(new Task.Modal(event.getProject(), "Connecting to Google Drive", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    connectAndDisplayTokens(indicator);
                } catch (Exception e) {
                    LOG.error("Failed to connect to Google Drive", e);
                    Messages.showErrorDialog(
                            "Failed to connect to Google Drive: " + e.getMessage(),
                            "Connection Error"
                    );
                }
            }
        });
    }

    private void connectAndDisplayTokens(ProgressIndicator indicator) throws Exception {
        // Create temporary directory for credentials
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "mesfavoris-gdrive-test");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        // Create and initialize connection manager
        GDriveConnectionManager connectionManager = new GDriveConnectionManager(
                tempDir,
                "Mes Favoris",
                "mesfavoris-test"
        );
        
        try {
            connectionManager.init();
            
            // Connect to Google Drive
            indicator.setText("Connecting to Google Drive...");
            connectionManager.connect(indicator);
            
            // Read the stored credentials
            FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(tempDir);
            DataStore<StoredCredential> dataStore = dataStoreFactory.getDataStore(DEFAULT_DATA_STORE_ID);
            StoredCredential storedCredential = dataStore.get("user");
            
            if (storedCredential != null) {
                String accessToken = storedCredential.getAccessToken();
                String refreshToken = storedCredential.getRefreshToken();
                Long expirationTime = storedCredential.getExpirationTimeMilliseconds();
                
                // Log tokens
                LOG.warn("=== GOOGLE DRIVE TOKENS ===");
                LOG.warn("Access Token: " + accessToken);
                LOG.warn("Refresh Token: " + refreshToken);
                LOG.warn("Expiration Time: " + expirationTime);
                LOG.warn("===========================");
                
                // Display tokens in dialog
                String message = String.format(
                        "Successfully connected to Google Drive!\n\n" +
                        "Access Token:\n%s\n\n" +
                        "Refresh Token:\n%s\n\n" +
                        "Expiration Time: %d\n\n" +
                        "Tokens have been logged to the console.",
                        truncate(accessToken, 50),
                        truncate(refreshToken, 50),
                        expirationTime
                );
                
                Messages.showInfoMessage(message, "Google Drive Connected");
            } else {
                Messages.showWarningDialog("Connected but no credentials found", "Warning");
            }
            
            // Disconnect
            connectionManager.disconnect(indicator);
            
        } finally {
            connectionManager.close();
        }
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}

