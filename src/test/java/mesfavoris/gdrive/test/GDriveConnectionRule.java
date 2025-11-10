package mesfavoris.gdrive.test;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import mesfavoris.gdrive.GDriveTestUser;
import mesfavoris.gdrive.connection.GDriveConnectionManager;
import mesfavoris.gdrive.connection.NoAuthorizationCodeInstalledApp;
import mesfavoris.gdrive.connection.auth.IAuthorizationCodeInstalledAppProvider;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.util.Random;

import static com.google.api.client.auth.oauth2.StoredCredential.DEFAULT_DATA_STORE_ID;

public class GDriveConnectionRule extends ExternalResource {

	private GDriveConnectionManager gDriveConnectionManager;
	private MemoryDataStoreFactory dataStoreFactory;
	private final boolean connect;
	private final IAuthorizationCodeInstalledAppProvider authorizationCodeProvider;
	private final GDriveTestUser user;

	public GDriveConnectionRule(GDriveTestUser user, boolean connect) {
		this.user = user;
		this.connect = connect;
		this.authorizationCodeProvider = new NoAuthorizationCodeInstalledApp.Provider();
	}

	@Override
	public void before() throws Exception {
		dataStoreFactory = new MemoryDataStoreFactory();

		if (user.getCredential().isPresent()) {
			addCredentialToDataStore(user.getCredential().get());
		}

		String applicationFolderName = "gdriveConnectionManagerTest" + new Random().nextInt(1000);
		InMemoryGDriveUserInfoStore userInfoStore = new InMemoryGDriveUserInfoStore();
		gDriveConnectionManager = new GDriveConnectionManager(dataStoreFactory, authorizationCodeProvider,
				userInfoStore, "mes favoris", applicationFolderName);
		gDriveConnectionManager.init();
		if (connect) {
			connect();
		}
	}

	private void addCredentialToDataStore(StoredCredential credential) throws IOException {
		DataStore<StoredCredential> dataStore = dataStoreFactory.getDataStore(DEFAULT_DATA_STORE_ID);
		dataStore.set("user", credential);
	}
	
	public String getApplicationFolderId() {
		return gDriveConnectionManager.getApplicationFolderId();
	}

	public GDriveConnectionManager getGDriveConnectionManager() {
		return gDriveConnectionManager;
	}

	public Drive getDrive() {
		return gDriveConnectionManager.getDrive();
	}

	public void connect() throws IOException {
		gDriveConnectionManager.connect(new EmptyProgressIndicator());
	}

	public void disconnect() throws IOException {
		gDriveConnectionManager.disconnect(new EmptyProgressIndicator());
	}

	@Override
	public void after() {
		try {
			if (gDriveConnectionManager == null) {
				return;
			}
			connect();
			deleteApplicationFolder();
			disconnect();
			gDriveConnectionManager.close();
		} catch (Exception e) {
			// ignore
		}
	}

	private void deleteApplicationFolder() throws IOException {
		gDriveConnectionManager.getDrive().files().delete(gDriveConnectionManager.getApplicationFolderId()).execute();
	}

}
