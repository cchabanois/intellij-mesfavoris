package mesfavoris.gdrive.connection;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.User;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.gdrive.connection.auth.AuthorizationCodeIntellijApp;
import mesfavoris.gdrive.connection.auth.CancellableLocalServerReceiver;
import mesfavoris.gdrive.connection.auth.IAuthorizationCodeInstalledAppProvider;
import mesfavoris.gdrive.connection.store.PasswordSafeDataStoreFactory;
import mesfavoris.remote.IRemoteBookmarksStore.State;
import mesfavoris.remote.UserInfo;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages connection to GDrive. A folder is created for the application.
 * 
 * @author cchabanois
 *
 */
public class GDriveConnectionManager {
	private static final Logger LOG = Logger.getInstance(GDriveConnectionManager.class);

	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	private final Project project;
	private HttpTransport httpTransport;
	private final String applicationName;
	private final IAuthorizationCodeInstalledAppProvider authorizationCodeInstalledAppProvider;
	private final String applicationFolderName;
	private final AtomicReference<State> state = new AtomicReference<>(State.disconnected);
    private final DataStoreFactory dataStoreFactory;
	private final IGDriveUserInfoStore userInfoStore;
	private final GoogleOAuthClientConfig googleOAuthClientConfig;
	private Drive drive;
	private String applicationFolderId;
	private UserInfo userInfo;

	/**
	 *
	 * @param project
	 *            the project
	 * @param applicationName
	 *            the application name to be used in the UserAgent header of
	 *            each request
	 * @param applicationFolderName
	 *            the folder name for the application on GDrive. It will be
	 *            created after connection if it does not exist
	 */
	public GDriveConnectionManager(Project project, String applicationName, String applicationFolderName) {
		this(project, new PasswordSafeDataStoreFactory(), new AuthorizationCodeIntellijApp.Provider(),
				project.getService(GDriveUserInfoStore.class),
				project.getService(GoogleOAuthClientConfigStore.class).getConfig(),
				applicationName, applicationFolderName);
	}

	public GDriveConnectionManager(Project project,
									DataStoreFactory dataStoreFactory,
			                       IAuthorizationCodeInstalledAppProvider authorizationCodeInstalledAppProvider,
                                   IGDriveUserInfoStore userInfoStore,
                                   GoogleOAuthClientConfig googleOAuthClientConfig,
                                   String applicationName,
                                   String applicationFolderName) {
		this.project = project;
        this.dataStoreFactory = dataStoreFactory;
		this.authorizationCodeInstalledAppProvider = authorizationCodeInstalledAppProvider;
		this.userInfoStore = userInfoStore;
		this.googleOAuthClientConfig = googleOAuthClientConfig;
		this.applicationName = applicationName;
		this.applicationFolderName = applicationFolderName;
	}

	public void init() throws GeneralSecurityException, IOException {
		httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		UserInfo user = loadUserInfo();
		synchronized (this) {
			this.userInfo = user;
		}
	}

	public void close() throws IOException {
		httpTransport.shutdown();
	}

	public String getApplicationFolderName() {
		return applicationFolderName;
	}

	/**
	 * Get info about user currently associated to the manager
	 * 
	 * @return the user or null if unknown
	 */
	public synchronized UserInfo getUserInfo() {
		return userInfo;
	}

	public void connect(ProgressIndicator progressIndicator) throws IOException {
		if (!state.compareAndSet(State.disconnected, State.connecting)) {
			return;
		}
		try {
			if (progressIndicator != null) {
				progressIndicator.setFraction(0.0);
			}
			Credential credential = authorize(progressIndicator);
			if (progressIndicator != null) {
				progressIndicator.setFraction(0.85);
			}
			Drive drive = new Drive.Builder(httpTransport, JSON_FACTORY,
					new GDriveBackOffHttpRequestInitializer(credential)).setApplicationName(applicationName).build();
			String bookmarkDirId = getApplicationFolderId(drive);
			UserInfo authenticatedUser = getAuthenticatedUser(drive, progressIndicator);
			if (progressIndicator != null) {
				progressIndicator.setFraction(0.95);
			}
			saveUser(authenticatedUser, progressIndicator);
			synchronized (this) {
				this.drive = drive;
				this.applicationFolderId = bookmarkDirId;
				this.userInfo = authenticatedUser;
			}
			state.set(State.connected);
			if (progressIndicator != null) {
				progressIndicator.setFraction(1.0);
			}
			fireConnected();
		} finally {
			if (state.compareAndSet(State.connecting, State.disconnected)) {
				synchronized (this) {
					this.drive = null;
					this.applicationFolderId = null;
				}
			}
		}
	}

	public synchronized Drive getDrive() {
		return drive;
	}

	public synchronized String getApplicationFolderId() {
		return applicationFolderId;
	}

	private String getApplicationFolderId(Drive drive) throws IOException {
		Files.List request = drive.files().list()
				.setQ("mimeType='application/vnd.google-apps.folder' and trashed=false and title='"
						+ applicationFolderName + "' and 'root' in parents");
		FileList files = request.execute();
		if (files.getItems().isEmpty()) {
			return createApplicationFolder(drive);
		} else {
			return files.getItems().get(0).getId();
		}
	}

	private String createApplicationFolder(Drive drive) throws IOException {
		com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
		body.setTitle(applicationFolderName);
		body.setMimeType("application/vnd.google-apps.folder");
		com.google.api.services.drive.model.File file = drive.files().insert(body).execute();
		return file.getId();
	}

	public State getState() {
		return state.get();
	}

	private void fireConnected() {
		project.getMessageBus().syncPublisher(GDriveConnectionListener.TOPIC).connected();
	}

	private void fireDisconnected() {
		project.getMessageBus().syncPublisher(GDriveConnectionListener.TOPIC).disconnected();
	}

	private Credential authorize(final ProgressIndicator progressIndicator) throws IOException {
		if (progressIndicator != null) {
			progressIndicator.setText("Authorizes the application to access user's protected data on Google Drive");
		}
		// Create client secrets from GoogleOAuthClientConfig
		// In this context, the client secret is obviously not treated as a secret
		// because it's embedded in the application and can be extracted by users.
		GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
		details.setClientId(googleOAuthClientConfig.getClientId());
		details.setClientSecret(googleOAuthClientConfig.getClientSecret());

		GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
		clientSecrets.setInstalled(details);

		// set up authorization code flow
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY,
				clientSecrets, Collections.singleton(DriveScopes.DRIVE)).setDataStoreFactory(dataStoreFactory).build();
		// authorize
		LocalServerReceiver localServerReceiver = new LocalServerReceiver();
		CancellableLocalServerReceiver cancellableReceiver = new CancellableLocalServerReceiver(
				localServerReceiver, progressIndicator);
		AuthorizationCodeInstalledApp authorizationCodeInstalledApp = authorizationCodeInstalledAppProvider.get(flow,
				cancellableReceiver, progressIndicator);
		return authorizationCodeInstalledApp.authorize("user");
	}

	private UserInfo getAuthenticatedUser(Drive drive, ProgressIndicator progressIndicator) throws IOException {
		if (progressIndicator != null) {
			progressIndicator.setText("Getting authenticated user info");
		}
		About about = drive.about().get().execute();
		User user = about.getUser();
		return new UserInfo(user.getEmailAddress(), user.getDisplayName());
	}

	public void disconnect(ProgressIndicator progressIndicator) {
		if (!state.compareAndSet(State.connected, State.disconnected)) {
			return;
		}
		synchronized (this) {
			this.drive = null;
			this.applicationFolderId = null;
		}
		fireDisconnected();
	}



	private UserInfo loadUserInfo() {
		return userInfoStore.getUserInfo();
	}

	private void saveUser(UserInfo user, ProgressIndicator progressIndicator) {
		if (progressIndicator != null) {
			progressIndicator.setText("Saving user info");
		}
		userInfoStore.setUserInfo(user);
	}

	public void deleteCredentials() throws IOException {
		if (getState() != State.disconnected) {
			throw new IOException("Cannot delete credentials while connected");
		}
		synchronized (this) {
			this.userInfo = null;
		}
		userInfoStore.setUserInfo(null);
		DataStore<StoredCredential> dataStore = dataStoreFactory.getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID);
		dataStore.clear();
	}
}
