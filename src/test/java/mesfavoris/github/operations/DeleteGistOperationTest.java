package mesfavoris.github.operations;
import mesfavoris.github.client.GistResponse;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DeleteGistOperationTest extends AbstractGithubOperationTest {

    @Test
    public void testDeleteGist_deletesGist() throws Exception {
        GistResponse created = apiClient.createGist("test", "bookmarks.json", "{}");
        DeleteGistOperation op = new DeleteGistOperation(apiClient);

        op.deleteGist(created.id, null);

        assertThatThrownBy(() -> apiClient.loadGist(created.id))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("404");
    }
}
