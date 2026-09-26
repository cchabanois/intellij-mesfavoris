package mesfavoris.github.test;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import mesfavoris.github.GithubTestUser;
import mesfavoris.github.connection.GithubConnectionManager;
import mesfavoris.github.connection.GithubUserInfoStore;
import mesfavoris.github.integration.IGithubAccountResolver;
import mesfavoris.github.mappings.GistMapping;
import mesfavoris.github.mappings.GistMappingsStore;
import mesfavoris.github.client.GistApiClient;
import mesfavoris.remote.IRemoteBookmarksStore.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Optional;

/**
 * Sets up a GitHub connection for tests. When {@code USER1_GITHUB_TOKEN} is set the whole chain talks to the
 * real GitHub API; otherwise it talks to an in-memory {@link FakeGistApiServer}, so the tests run offline
 * without hitting — and risking a ban from — the real API. The switch is transparent to callers: the account
 * resolver simply hands the connection manager the fake's base URL instead of {@code api.github.com}.
 */
public class GithubConnectionRule extends ExternalResource {

    private final Project project;
    private final GithubTestUser user;
    private final boolean connect;

    private GithubConnectionManager connectionManager;
    private GistMappingsStore gistMappingsStore;
    private FakeGistApiServer fakeServer;

    public GithubConnectionRule(Project project, GithubTestUser user, boolean connect) {
        this.project = project;
        this.user = user;
        this.connect = connect;
    }

    @Override
    public void before() throws Exception {
        String token;
        String apiBaseUrl;
        Optional<String> realToken = user.getToken();
        if (realToken.isPresent()) {
            token = realToken.get();
            apiBaseUrl = user.getApiBaseUrl();
        } else {
            fakeServer = new FakeGistApiServer();
            fakeServer.start();
            token = FakeGistApiServer.TOKEN;
            apiBaseUrl = fakeServer.baseUrl();
        }

        ServiceContainerUtil.registerServiceInstance(
                ApplicationManager.getApplication(),
                IGithubAccountResolver.class,
                new TestGithubAccountResolver(token, apiBaseUrl));

        GithubUserInfoStore userInfoStore = new GithubUserInfoStore();
        connectionManager = new GithubConnectionManager(project, userInfoStore);
        connectionManager.init();

        gistMappingsStore = new GistMappingsStore(project);

        if (connect) {
            connect();
        }
    }

    @Override
    public void after() {
        try {
            if (connectionManager == null) {
                return;
            }
            deleteCreatedGists();
            connectionManager.disconnect(null);
        } catch (Exception e) {
            // ignore
        } finally {
            if (fakeServer != null) {
                fakeServer.stop();
            }
        }
    }

    /**
     * Deletes the gists we created. Required for the real API (gists persist on the account); redundant for
     * the fake (its state is discarded on stop), but run in both modes so the cleanup path stays uniform.
     */
    private void deleteCreatedGists() {
        try {
            if (connectionManager.getState() != State.connected) {
                connect();
            }
            String token = connectionManager.getAccessToken();
            String apiBaseUrl = connectionManager.getApiBaseUrl();
            GistApiClient apiClient = new GistApiClient(() -> token, () -> apiBaseUrl,
                    HttpClient.newHttpClient());
            for (GistMapping mapping : gistMappingsStore.getMappings()) {
                try {
                    apiClient.deleteGist(mapping.getGistId());
                } catch (IOException e) {
                    // ignore — gist may already be deleted
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public void connect() throws IOException {
        connectionManager.connect(new EmptyProgressIndicator());
    }

    public void disconnect() {
        connectionManager.disconnect(null);
    }

    public GithubConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public GistMappingsStore getGistMappingsStore() {
        return gistMappingsStore;
    }

    private static class TestGithubAccountResolver implements IGithubAccountResolver {
        private final String token;
        private final String apiBaseUrl;

        TestGithubAccountResolver(String token, String apiBaseUrl) {
            this.token = token;
            this.apiBaseUrl = apiBaseUrl;
        }

        @Nullable
        @Override
        public GithubAccountInfo resolveAccount(@NotNull Project project) {
            return new GithubAccountInfo(token, apiBaseUrl, "test-user");
        }
    }
}
