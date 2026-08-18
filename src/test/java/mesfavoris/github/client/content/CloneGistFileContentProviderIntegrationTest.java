package mesfavoris.github.client.content;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.github.GithubTestUser;
import mesfavoris.github.client.GistApiClient;
import mesfavoris.github.client.GistFile;
import mesfavoris.github.client.GistResponse;
import org.junit.Assume;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real git clone in {@link CloneGistFileContentProvider} against a live Gist. Needs an IDE and
 * {@code USER1_GITHUB_TOKEN}, so it is skipped when the token is absent.
 */
public class CloneGistFileContentProviderIntegrationTest extends BasePlatformTestCase {

    private GistApiClient apiClient;
    private String token;
    private String createdGistId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Assume.assumeTrue("USER1_GITHUB_TOKEN not set", GithubTestUser.USER1.getToken().isPresent());
        token = GithubTestUser.USER1.getToken().get();
        String apiBaseUrl = GithubTestUser.USER1.getApiBaseUrl();
        HttpClient httpClient = GistApiClient.newHttpClient();
        apiClient = new GistApiClient(() -> token, () -> apiBaseUrl, httpClient, "", null);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (createdGistId != null && apiClient != null) {
                apiClient.deleteGist(createdGistId);
            }
        } finally {
            super.tearDown();
        }
    }

    public void testGetFileContent() throws Exception {
        String content = "{\"version\":\"1.0\",\"note\":\"cloned via provider\"}";
        GistResponse created = apiClient.createGist("clone provider test", "bookmarks.json", content);
        createdGistId = created.id;
        GistFile file = created.files.get("bookmarks.json");
        CloneGistFileContentProvider provider =
                new CloneGistFileContentProvider(getProject(), () -> token);

        byte[] result = provider.getFileContent(created, file);

        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo(content);
    }
}
