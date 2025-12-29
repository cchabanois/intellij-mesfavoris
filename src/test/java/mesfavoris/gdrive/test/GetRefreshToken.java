package mesfavoris.gdrive.test;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.gdrive.connection.GDriveConnectionManager;
import mesfavoris.gdrive.connection.GoogleOAuthClientConfig;
import mesfavoris.gdrive.connection.auth.AuthorizationCodeIntellijApp;

import static com.google.api.client.auth.oauth2.StoredCredential.DEFAULT_DATA_STORE_ID;

/**
 * Utility test to get refresh token for Google Drive API.
 * This test is skipped by default on CI/automated runs.
 *
 * To run manually from command line:
 *   RUN_MANUAL_TEST=true ./gradlew test --tests "mesfavoris.gdrive.test.GetRefreshToken"
 *
 * Or from IntelliJ IDEA:
 *   Add environment variable in Run Configuration: RUN_MANUAL_TEST=true
 */
public class GetRefreshToken extends BasePlatformTestCase {

    private boolean manualTestEnabled;

    @Override
    protected void setUp() throws Exception {
        manualTestEnabled = "true".equalsIgnoreCase(System.getenv("RUN_MANUAL_TEST"));
        if (!manualTestEnabled) {
            // Don't call super.setUp() to avoid initializing the test environment
            return;
        }
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        if (!manualTestEnabled) {
            // Don't call super.tearDown() if we didn't call super.setUp()
            return;
        }
        super.tearDown();
    }

    public void testGetRefreshToken() throws Exception {
        // Skip this test unless explicitly enabled
        if (!manualTestEnabled) {
            System.out.println("Skipping GetRefreshToken - set RUN_MANUAL_TEST=true to run");
            return;
        }

        getRefreshToken();
    }

    private void getRefreshToken() throws Exception {
        InMemoryGDriveUserInfoStore userInfoStore = new InMemoryGDriveUserInfoStore();
        MemoryDataStoreFactory dataStoreFactory = new MemoryDataStoreFactory();

        GDriveConnectionManager connectionManager = new GDriveConnectionManager(
                dataStoreFactory, new AuthorizationCodeIntellijApp.Provider(),
                userInfoStore,
                GoogleOAuthClientConfig.getDefault(),
                "Mes Favoris",
                "mesfavoris-test"
        );

        connectionManager.init();

        ProgressIndicator indicator = new EmptyProgressIndicator();
        connectionManager.connect(indicator);

        DataStore<StoredCredential> dataStore = dataStoreFactory.getDataStore(DEFAULT_DATA_STORE_ID);
        StoredCredential storedCredential = dataStore.get("user");

        if (storedCredential != null) {
            String accessToken = storedCredential.getAccessToken();
            String refreshToken = storedCredential.getRefreshToken();
            Long expirationTime = storedCredential.getExpirationTimeMilliseconds();

            System.out.println("=== Google Drive Credentials ===");
            System.out.println("User: " + connectionManager.getUserInfo().getDisplayName() + " (" + connectionManager.getUserInfo().getEmailAddress() + ")");
            System.out.println("Access Token: " + accessToken);
            System.out.println("Refresh Token: " + refreshToken);
            System.out.println("Expiration Time: " + expirationTime);
            System.out.println("================================");
        } else {
            System.out.println("No stored credentials found");
        }
    }

}
