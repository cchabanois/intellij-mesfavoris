package mesfavoris.github.operations;
import mesfavoris.github.client.GistResponse;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateGistOperationTest extends AbstractGithubOperationTest {

    @Test
    public void testCreateGist_usesBookmarkFolderNameAsDescription() throws Exception {
        CreateGistOperation op = new CreateGistOperation(apiClient);

        GistResponse response = op.createGist("My Folder",
                "{}".getBytes(StandardCharsets.UTF_8), null);
        trackGist(response.id);

        assertThat(response.description).isEqualTo("mesfavoris: My Folder");
        assertThat(response.files).containsKey("bookmarks.json");
    }

    @Test
    public void testCreateGist_encodesContentAsUtf8() throws Exception {
        CreateGistOperation op = new CreateGistOperation(apiClient);
        String content = "{\"name\":\"café\"}";

        GistResponse response = op.createGist("UTF-8 Test",
                content.getBytes(StandardCharsets.UTF_8), null);
        trackGist(response.id);

        GistResponse loaded = apiClient.loadGist(response.id);
        assertThat(loaded.files.get("bookmarks.json").content).isEqualTo(content);
    }
}
