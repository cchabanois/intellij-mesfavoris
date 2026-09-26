package mesfavoris.github.operations;

import mesfavoris.github.GithubTestUser;
import mesfavoris.github.client.GistApiClient;
import mesfavoris.github.client.content.DefaultGistFileContentProvider;
import mesfavoris.github.test.FakeGistApiServer;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base for Gist API operation tests. Runs against the real GitHub API when {@code USER1_GITHUB_TOKEN} is set,
 * and otherwise against an in-memory {@link FakeGistApiServer} so the tests run offline — without hitting, and
 * risking a ban from, the real API.
 */
public abstract class AbstractGithubOperationTest {

    protected GistApiClient apiClient;
    private FakeGistApiServer fakeServer;
    private final List<String> createdGistIds = new ArrayList<>();

    @Before
    public void setUp() {
        Optional<String> token = GithubTestUser.USER1.getToken();
        if (token.isPresent()) {
            String apiBaseUrl = GithubTestUser.USER1.getApiBaseUrl();
            HttpClient httpClient = GistApiClient.newHttpClient();
            apiClient = new GistApiClient(token::get, () -> apiBaseUrl, httpClient, "",
                    DefaultGistFileContentProvider.create(null, httpClient, token::get));
        } else {
            fakeServer = new FakeGistApiServer();
            fakeServer.start();
            apiClient = fakeServer.newApiClient();
        }
    }

    @After
    public void tearDown() throws IOException {
        if (fakeServer != null) {
            fakeServer.stop();
            return;
        }
        // Real API: clean up the gists we created.
        for (String id : createdGistIds) {
            apiClient.deleteGist(id);
        }
    }

    protected String trackGist(String gistId) {
        createdGistIds.add(gistId);
        return gistId;
    }
}
