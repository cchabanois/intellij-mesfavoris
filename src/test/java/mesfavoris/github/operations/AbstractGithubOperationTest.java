package mesfavoris.github.operations;

import mesfavoris.github.GithubTestUser;
import mesfavoris.github.client.GistApiClient;
import mesfavoris.github.client.content.DefaultGistFileContentProvider;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractGithubOperationTest {

    protected GistApiClient apiClient;
    private final List<String> createdGistIds = new ArrayList<>();

    @Before
    public void setUp() {
        Assume.assumeTrue("USER1_GITHUB_TOKEN not set", GithubTestUser.USER1.getToken().isPresent());
        String token = GithubTestUser.USER1.getToken().get();
        String apiBaseUrl = GithubTestUser.USER1.getApiBaseUrl();
        // No IDE/project in these plain-JUnit tests, so getFileContent uses the small + raw-HTTP chain,
        // sharing a single (redirect-following) HttpClient with the API client.
        HttpClient httpClient = GistApiClient.newHttpClient();
        apiClient = new GistApiClient(() -> token, () -> apiBaseUrl, httpClient, "",
                DefaultGistFileContentProvider.create(null, httpClient, () -> token));
    }

    @After
    public void tearDown() throws IOException {
        for (String id : createdGistIds) {
            apiClient.deleteGist(id);
        }
    }

    protected String trackGist(String gistId) {
        createdGistIds.add(gistId);
        return gistId;
    }
}
