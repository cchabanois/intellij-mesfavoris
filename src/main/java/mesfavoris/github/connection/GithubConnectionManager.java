package mesfavoris.github.connection;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.github.integration.IGithubAccountResolver;
import mesfavoris.github.operations.GetAuthenticatedUserOperation;
import mesfavoris.github.operations.GistApiClient;
import mesfavoris.remote.IRemoteBookmarksStore.State;
import mesfavoris.remote.RemoteStoreConfigurationException;
import mesfavoris.remote.UserInfo;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the GitHub connection lifecycle (disconnected / connecting / connected).
 * The access token and API base URL are cached for the duration of the session; they are
 * cleared on disconnect so that a reconnect re-resolves the account (handles token rotation
 * or account switches transparently).
 */
public class GithubConnectionManager {
    private static final Logger LOG = Logger.getInstance(GithubConnectionManager.class);

    private final Project project;
    private final GithubUserInfoStore userInfoStore;
    private final AtomicReference<State> state = new AtomicReference<>(State.disconnected);

    private volatile String accessToken;
    private volatile String apiBaseUrl;
    private volatile UserInfo userInfo;

    public GithubConnectionManager(Project project) {
        this(project, project.getService(GithubUserInfoStore.class));
    }

    public GithubConnectionManager(Project project, GithubUserInfoStore userInfoStore) {
        this.project = project;
        this.userInfoStore = userInfoStore;
    }

    public void init() {
        this.userInfo = userInfoStore.getUserInfo();
    }

    public void connect(ProgressIndicator indicator) throws IOException {
        if (!state.compareAndSet(State.disconnected, State.connecting)) {
            return;
        }
        try {
            if (indicator != null) {
                indicator.setFraction(0.0);
                indicator.setText("Connecting to GitHub...");
            }

            IGithubAccountResolver accountIntegration = getAccountIntegration();
            if (accountIntegration == null) {
                throw new IOException(
                        "GitHub plugin is not installed. Please install the GitHub plugin to use GitHub Gists sync.");
            }

            IGithubAccountResolver.GithubAccountInfo accountInfo = accountIntegration.resolveAccount(project);
            if (accountInfo == null) {
                throw new RemoteStoreConfigurationException(
                        "No GitHub account available. Please configure a GitHub account in " +
                        "Settings > Version Control > GitHub.",
                        org.jetbrains.plugins.github.ui.GithubSettingsConfigurable.class);
            }

            if (indicator != null) {
                indicator.setFraction(0.5);
                indicator.setText("Verifying GitHub token...");
            }

            GistApiClient apiClient = new GistApiClient(
                    accountInfo::accessToken, accountInfo::apiBaseUrl, HttpClient.newHttpClient());
            UserInfo authenticatedUser = new GetAuthenticatedUserOperation(apiClient)
                    .getAuthenticatedUser(indicator);

            userInfoStore.setUserInfo(authenticatedUser);
            this.accessToken = accountInfo.accessToken();
            this.apiBaseUrl = accountInfo.apiBaseUrl();
            this.userInfo = authenticatedUser;

            state.set(State.connected);

            if (indicator != null) {
                indicator.setFraction(1.0);
            }

            fireConnected();
        } finally {
            if (state.compareAndSet(State.connecting, State.disconnected)) {
                this.accessToken = null;
                this.apiBaseUrl = null;
            }
        }
    }

    public void disconnect(ProgressIndicator indicator) {
        if (!state.compareAndSet(State.connected, State.disconnected)) {
            return;
        }
        this.accessToken = null;
        this.apiBaseUrl = null;
        fireDisconnected();
    }

    public State getState() {
        return state.get();
    }

    @Nullable
    public UserInfo getUserInfo() {
        return userInfo;
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    @Nullable
    public String getApiBaseUrl() {
        return apiBaseUrl != null ? apiBaseUrl : "https://api.github.com";
    }

    @Nullable
    private IGithubAccountResolver getAccountIntegration() {
        return ApplicationManager.getApplication().getService(IGithubAccountResolver.class);
    }

    private void fireConnected() {
        project.getMessageBus().syncPublisher(GithubConnectionListener.TOPIC).connected();
    }

    private void fireDisconnected() {
        project.getMessageBus().syncPublisher(GithubConnectionListener.TOPIC).disconnected();
    }
}
