package mesfavoris.github.operations;

import mesfavoris.remote.ConflictException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class UpdateGistOperationTest extends AbstractGithubOperationTest {

    @Test
    public void testUpdateGist_success() throws Exception {
        GistApiClient.GistResponse created = apiClient.createGist("test", "bookmarks.json", "{}");
        trackGist(created.id);
        UpdateGistOperation op = new UpdateGistOperation(apiClient);

        op.updateGist(created.id, "{\"v\":1}".getBytes(StandardCharsets.UTF_8), null, null);

        GistApiClient.GistResponse loaded = apiClient.loadGist(created.id);
        assertThat(loaded.files.get("bookmarks.json").content).isEqualTo("{\"v\":1}");
    }

    @Test
    public void testUpdateGist_propagatesConflictException() throws Exception {
        GistApiClient.GistResponse created = apiClient.createGist("test", "bookmarks.json", "{}");
        trackGist(created.id);
        GistApiClient.GistResponse current = apiClient.loadGist(created.id);
        UpdateGistOperation op = new UpdateGistOperation(apiClient);
        // GitHub timestamps have second precision — sleep >1s so updated_at advances on the first update
        Thread.sleep(1100);
        op.updateGist(created.id, "{\"v\":1}".getBytes(StandardCharsets.UTF_8), current.updated_at, null);

        Throwable thrown = catchThrowable(() ->
                op.updateGist(created.id, "{\"v\":2}".getBytes(StandardCharsets.UTF_8),
                        current.updated_at, null));

        assertThat(thrown).isInstanceOf(ConflictException.class);
    }

    @Test
    public void testUpdateGist_withNullEtag_succeeds() throws Exception {
        GistApiClient.GistResponse created = apiClient.createGist("test", "bookmarks.json", "{}");
        trackGist(created.id);
        UpdateGistOperation op = new UpdateGistOperation(apiClient);

        GistApiClient.GistResponse updated = op.updateGist(created.id,
                "{\"v\":1}".getBytes(StandardCharsets.UTF_8), null, null);

        assertThat(updated.id).isEqualTo(created.id);
    }
}
