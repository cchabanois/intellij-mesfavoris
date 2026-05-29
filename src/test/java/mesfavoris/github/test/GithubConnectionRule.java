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
import mesfavoris.github.operations.GistApiClient;
import mesfavoris.remote.IRemoteBookmarksStore.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.net.http.HttpClient;

public class GithubConnectionRule extends ExternalResource {

    private final Project project;
    private final GithubTestUser user;
    private final boolean connect;

    private GithubConnectionManager connectionManager;
    private GistMappingsStore gistMappingsStore;

    public GithubConnectionRule(Project project, GithubTestUser user, boolean connect) {
        this.project = project;
        this.user = user;
        this.connect = connect;
    }

    @Override
    public void before() throws Exception {
        ServiceContainerUtil.registerServiceInstance(
                ApplicationManager.getApplication(),
                IGithubAccountResolver.class,
                new TestGithubAccountResolver(user));

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
            if (connectionManager.getState() != State.connected) {
                try {
                    connect();
                } catch (Exception e) {
                    return;
                }
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
            connectionManager.disconnect(null);
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
        private final GithubTestUser user;

        TestGithubAccountResolver(GithubTestUser user) {
            this.user = user;
        }

        @Nullable
        @Override
        public GithubAccountInfo resolveAccount(@NotNull Project project) {
            return user.getToken()
                    .map(token -> new GithubAccountInfo(token, user.getApiBaseUrl(), "test-user"))
                    .orElse(null);
        }
    }
}
