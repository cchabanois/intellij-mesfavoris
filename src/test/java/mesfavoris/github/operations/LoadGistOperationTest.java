package mesfavoris.github.operations;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class LoadGistOperationTest extends AbstractGithubOperationTest {

    @Test
    public void testLoadGist_success() throws Exception {
        String content = "{\"version\":\"1.0\"}";
        GistApiClient.GistResponse created = apiClient.createGist("test", "bookmarks.json", content);
        trackGist(created.id);
        LoadGistOperation op = new LoadGistOperation(apiClient);

        LoadGistOperation.GistContents result = op.loadGist(created.id, null);

        assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo(content);
        assertThat(result.etag()).isNotNull();
        assertThat(result.response().id).isEqualTo(created.id);
    }

    @Test
    public void testLoadGist_missingBookmarksJson_throwsIOException() throws Exception {
        GistApiClient.GistResponse created = apiClient.createGist("test", "other.json", "{}");
        trackGist(created.id);
        LoadGistOperation op = new LoadGistOperation(apiClient);

        Throwable thrown = catchThrowable(() -> op.loadGist(created.id, null));

        assertThat(thrown).isInstanceOf(IOException.class)
                .hasMessageContaining("bookmarks.json");
    }
}
