package mesfavoris.github.client.content;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.github.GithubTestUser;
import mesfavoris.github.client.GistApiClient;
import mesfavoris.github.client.GistFile;
import mesfavoris.github.client.GistResponse;
import mesfavoris.github.test.FakeGistApiServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the real git clone in {@link CloneGistFileContentProvider} (needs an IDE and a {@code git}
 * executable). Clones from the real GitHub when {@code USER1_GITHUB_TOKEN} is set, otherwise from the git
 * repositories served by {@link FakeGistApiServer}.
 */
public class CloneGistFileContentProviderIntegrationTest extends BasePlatformTestCase {

    private GistApiClient apiClient;
    private FakeGistApiServer fakeServer;
    private String token;
    private String createdGistId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Optional<String> realToken = GithubTestUser.USER1.getToken();
        if (realToken.isPresent()) {
            token = realToken.get();
            String apiBaseUrl = GithubTestUser.USER1.getApiBaseUrl();
            apiClient = new GistApiClient(() -> token, () -> apiBaseUrl, GistApiClient.newHttpClient(), "", null);
        } else {
            fakeServer = new FakeGistApiServer();
            fakeServer.start();
            token = FakeGistApiServer.TOKEN;
            apiClient = fakeServer.newApiClient();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (createdGistId != null && apiClient != null) {
                apiClient.deleteGist(createdGistId);
            }
            if (fakeServer != null) {
                fakeServer.stop();
            }
        } finally {
            super.tearDown();
        }
    }

    public void testGetFileContent() throws Exception {
        String content = "{\"version\":\"1.0\",\"note\":\"cloned via provider\"}";
        GistResponse created = createGist(content);
        GistFile file = created.files.get("bookmarks.json");
        CloneGistFileContentProvider provider =
                new CloneGistFileContentProvider(getProject(), () -> token);

        byte[] result = provider.getFileContent(created, file);

        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo(content);
    }

    public void testGetFileContent_returnsLatestContentAfterUpdate() throws Exception {
        GistResponse created = createGist("{\"v\":1}");
        GistResponse updated = apiClient.updateGist(created.id, "bookmarks.json", "{\"v\":2}", null);
        CloneGistFileContentProvider provider =
                new CloneGistFileContentProvider(getProject(), () -> token);

        byte[] result = provider.getFileContent(updated, updated.files.get("bookmarks.json"));

        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("{\"v\":2}");
    }

    public void testGetFileContent_missingFile_throwsIOException() throws Exception {
        GistResponse created = createGist("{}");
        GistFile missing = new GistFile();
        missing.filename = "missing.json";
        CloneGistFileContentProvider provider =
                new CloneGistFileContentProvider(getProject(), () -> token);

        assertThatThrownBy(() -> provider.getFileContent(created, missing))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("missing.json");
    }

    private GistResponse createGist(String content) throws IOException {
        GistResponse created = apiClient.createGist("clone provider test", "bookmarks.json", content);
        createdGistId = created.id;
        return created;
    }
}
