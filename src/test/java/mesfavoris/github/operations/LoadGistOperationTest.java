package mesfavoris.github.operations;

import mesfavoris.github.client.GistResponse;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class LoadGistOperationTest extends AbstractGithubOperationTest {

    @Test
    public void testLoadGist_success() throws Exception {
        String content = "{\"version\":\"1.0\"}";
        GistResponse created = apiClient.createGist("test", "bookmarks.json", content);
        trackGist(created.id);
        LoadGistOperation op = new LoadGistOperation(apiClient);

        LoadGistOperation.GistContents result = op.loadGist(created.id, null);

        assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo(content);
        assertThat(result.etag()).isNotNull();
        assertThat(result.response().id).isEqualTo(created.id);
    }

    @Test
    public void testLoadGist_truncatedLargeFile_returnsFullContent() throws Exception {
        String content = largeJson();
        GistResponse created = apiClient.createGist("large gist", "bookmarks.json", content);
        trackGist(created.id);
        LoadGistOperation op = new LoadGistOperation(apiClient);

        LoadGistOperation.GistContents result = op.loadGist(created.id, null);

        assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo(content);
    }

    /** Builds a JSON object larger than 1&nbsp;MB so the Gist API marks the file as truncated. */
    private static String largeJson() {
        StringBuilder sb = new StringBuilder("{\"version\":\"1.0\",\"items\":[");
        for (int i = 0; i < 20000; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"id\":").append(i).append(",\"note\":\"").append("x".repeat(50)).append("\"}");
        }
        return sb.append("]}").toString();
    }

    @Test
    public void testLoadGist_missingBookmarksJson_throwsIOException() throws Exception {
        GistResponse created = apiClient.createGist("test", "other.json", "{}");
        trackGist(created.id);
        LoadGistOperation op = new LoadGistOperation(apiClient);

        Throwable thrown = catchThrowable(() -> op.loadGist(created.id, null));

        assertThat(thrown).isInstanceOf(IOException.class)
                .hasMessageContaining("bookmarks.json");
    }
}
