package mesfavoris.github.operations;

import mesfavoris.remote.ConflictException;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

public class GistApiClientTest extends AbstractGithubOperationTest {

    // --- createGist ---

    @Test
    public void testCreateGist_success() throws Exception {
        GistApiClient.GistResponse response = apiClient.createGist("test gist", "bookmarks.json", "{}");
        trackGist(response.id);

        assertThat(response.id).isNotNull();
        assertThat(response.html_url).isNotNull();
        assertThat(response.owner).isNotNull();
        assertThat(response.updated_at).isNotNull();
    }

    // --- updateGist ---

    @Test
    public void testUpdateGist_conflict_whenEtagIsStale() throws Exception {
        GistApiClient.GistResponse created = apiClient.createGist("test gist", "bookmarks.json", "{}");
        trackGist(created.id);
        String staleEtag = created.updated_at;
        Thread.sleep(1100); // updated_at has second precision
        apiClient.updateGist(created.id, "bookmarks.json", "{\"v\":1}", null);

        Throwable thrown = catchThrowable(() ->
                apiClient.updateGist(created.id, "bookmarks.json", "{\"v\":2}", staleEtag));

        assertThat(thrown).isInstanceOf(ConflictException.class);
    }

    @Test
    public void testUpdateGist_success_whenEtagIsCurrent() throws Exception {
        GistApiClient.GistResponse created = apiClient.createGist("test gist", "bookmarks.json", "{}");
        trackGist(created.id);
        GistApiClient.GistResponse current = apiClient.loadGist(created.id);

        GistApiClient.GistResponse updated = apiClient.updateGist(created.id, "bookmarks.json",
                "{\"v\":1}", current.updated_at);

        assertThat(updated.updated_at).isNotEqualTo(current.updated_at);
    }

    @Test
    public void testUpdateGist_noConflictCheck_whenEtagIsNull() throws Exception {
        GistApiClient.GistResponse created = apiClient.createGist("test gist", "bookmarks.json", "{}");
        trackGist(created.id);

        GistApiClient.GistResponse updated = apiClient.updateGist(created.id, "bookmarks.json",
                "{\"v\":1}", null);

        assertThat(updated.id).isEqualTo(created.id);
    }

    // --- loadGist ---

    @Test
    public void testLoadGist_success() throws Exception {
        GistApiClient.GistResponse created = apiClient.createGist("test gist", "bookmarks.json",
                "{\"version\":\"1.0\"}");
        trackGist(created.id);

        GistApiClient.GistResponse loaded = apiClient.loadGist(created.id);

        assertThat(loaded.id).isEqualTo(created.id);
        assertThat(loaded.files.get("bookmarks.json").content).isEqualTo("{\"version\":\"1.0\"}");
    }

    @Test
    public void testLoadGist_notFound() {
        Throwable thrown = catchThrowable(() -> apiClient.loadGist("000000000000000000000000nonexistent"));

        assertThat(thrown).isInstanceOf(IOException.class)
                .hasMessageContaining("404");
    }

    // --- deleteGist ---

    @Test
    public void testDeleteGist_success() throws Exception {
        GistApiClient.GistResponse created = apiClient.createGist("test gist", "bookmarks.json", "{}");

        apiClient.deleteGist(created.id);

        assertThatThrownBy(() -> apiClient.loadGist(created.id))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("404");
    }

    @Test
    public void testDeleteGist_notFound_treatedAsSuccess() throws Exception {
        apiClient.deleteGist("000000000000000000000000nonexistent");
    }

    // --- conditionalGetEtag ---

    @Test
    public void testConditionalGetEtag_notModified_returnsNull() throws Exception {
        GistApiClient.GistResponse created = apiClient.createGist("test gist", "bookmarks.json", "{}");
        trackGist(created.id);
        String etag = apiClient.conditionalGetEtag(created.id, null);

        String result = apiClient.conditionalGetEtag(created.id, etag);

        assertThat(result).isNull();
    }

    @Test
    public void testConditionalGetEtag_modified_returnsNewEtag() throws Exception {
        GistApiClient.GistResponse created = apiClient.createGist("test gist", "bookmarks.json", "{}");
        trackGist(created.id);
        String etag = apiClient.conditionalGetEtag(created.id, null);
        apiClient.updateGist(created.id, "bookmarks.json", "{\"v\":1}", null);

        String newEtag = apiClient.conditionalGetEtag(created.id, etag);

        assertThat(newEtag).isNotNull().isNotEqualTo(etag);
    }

    // --- getAuthenticatedUser ---

    @Test
    public void testGetAuthenticatedUser_success() throws Exception {
        GistApiClient.UserResponse user = apiClient.getAuthenticatedUser();

        assertThat(user.login).isNotNull();
    }
}
