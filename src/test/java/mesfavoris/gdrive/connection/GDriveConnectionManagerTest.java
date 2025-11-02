package mesfavoris.gdrive.connection;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.gdrive.GDriveTestUser;
import mesfavoris.remote.IRemoteBookmarksStore.State;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import static com.google.api.client.auth.oauth2.StoredCredential.DEFAULT_DATA_STORE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class GDriveConnectionManagerTest extends BasePlatformTestCase {

    private TemporaryFolder temporaryFolder;
    private GDriveConnectionManager gDriveConnectionManager;
    private IConnectionListener connectionListener;
    private final GDriveTestUser user = GDriveTestUser.USER1;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();
        connectionListener = mock(IConnectionListener.class);

        File dataStoreDir = temporaryFolder.newFolder();
        if (user.getCredential().isPresent()) {
            addCredentialToDataStore(dataStoreDir, user.getCredential().get());
        }
        String applicationFolderName = "gdriveConnectionManagerTest" + new Random().nextInt(1000);
        gDriveConnectionManager = new GDriveConnectionManager(dataStoreDir, new NoAuthorizationCodeInstalledApp.Provider(), "mes favoris", applicationFolderName);
        gDriveConnectionManager.init();
        gDriveConnectionManager.addConnectionListener(connectionListener);
    }

    private void addCredentialToDataStore(File dataStoreDir, StoredCredential credential) throws IOException {
        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(dataStoreDir);
        DataStore<StoredCredential> dataStore = dataStoreFactory.getDataStore(DEFAULT_DATA_STORE_ID);
        dataStore.set("user", credential);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            deleteApplicationFolder();
            gDriveConnectionManager.removeConnectionListener(connectionListener);
            gDriveConnectionManager.close();
            temporaryFolder.delete();
        } finally {
            super.tearDown();
        }
    }

    private void deleteApplicationFolder() throws IOException {
        gDriveConnectionManager.connect(new EmptyProgressIndicator());
        gDriveConnectionManager.getDrive().files().delete(gDriveConnectionManager.getApplicationFolderId());
    }

    public void testConnect() throws Exception {
        // Given

        // When
        gDriveConnectionManager.connect(new EmptyProgressIndicator());

        // Then
        assertThat(gDriveConnectionManager.getState()).isEqualTo(State.connected);
        verify(connectionListener).connected();
        assertThat(gDriveConnectionManager.getApplicationFolderId()).isNotNull();
        assertThat(gDriveConnectionManager.getUserInfo().getEmailAddress()).isEqualTo(user.getEmail());
        assertThat(gDriveConnectionManager.getUserInfo().getDisplayName()).isNotNull();
    }

    public void testDisconnect() throws Exception {
        // Given
        gDriveConnectionManager.connect(new EmptyProgressIndicator());

        // When
        gDriveConnectionManager.disconnect(new EmptyProgressIndicator());

        // Then
        assertThat(gDriveConnectionManager.getState()).isEqualTo(State.disconnected);
        verify(connectionListener).disconnected();
        assertThat(gDriveConnectionManager.getUserInfo()).isNotNull();
    }

}