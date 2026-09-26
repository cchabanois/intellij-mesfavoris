package mesfavoris.github.operations;
import mesfavoris.github.client.GistResponse;

import mesfavoris.remote.ConflictException;
import mesfavoris.tests.commons.waits.Waiter;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class UpdateGistOperationTest extends AbstractGithubOperationTest {

    @Test
    public void testUpdateGist_success() throws Exception {
        GistResponse created = apiClient.createGist("test", "bookmarks.json", "{}");
        trackGist(created.id);
        UpdateGistOperation op = new UpdateGistOperation(apiClient);

        op.updateGist(created.id, "{\"v\":1}".getBytes(StandardCharsets.UTF_8), null, null);

        // GitHub's Gist GET endpoint can serve stale content for a short window after a PATCH,
        // so poll until the fresh load reflects the update instead of reading exactly once.
        Waiter.waitUntil("Gist content was not updated", () ->
                        "{\"v\":1}".equals(apiClient.loadGist(created.id).files.get("bookmarks.json").content),
                Duration.ofSeconds(10));
    }

    @Test
    public void testUpdateGist_propagatesConflictException() throws Exception {
        GistResponse created = apiClient.createGist("test", "bookmarks.json", "{}");
        trackGist(created.id);
        GistResponse current = apiClient.loadGist(created.id);
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
        GistResponse created = apiClient.createGist("test", "bookmarks.json", "{}");
        trackGist(created.id);
        UpdateGistOperation op = new UpdateGistOperation(apiClient);

        GistResponse updated = op.updateGist(created.id,
                "{\"v\":1}".getBytes(StandardCharsets.UTF_8), null, null);

        assertThat(updated.id).isEqualTo(created.id);
    }
}
