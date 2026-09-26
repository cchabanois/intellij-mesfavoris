package mesfavoris.github.test;

import mesfavoris.github.client.GistApiClient;
import mesfavoris.github.client.GistResponse;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Checks that the gists served by {@link FakeGistApiServer} are real git repositories, kept in sync with REST. */
public class FakeGistGitServerTest {

    @Rule
    public final FakeGistApiServer gistApi = new FakeGistApiServer();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private GistApiClient apiClient;

    @Before
    public void setUp() {
        apiClient = gistApi.newApiClient();
    }

    @Test
    public void testClone_returnsGistFiles() throws Exception {
        GistResponse gist = apiClient.createGist("test", "bookmarks.json", "{\"v\":1}");

        try (Git git = clone(gist, FakeGistApiServer.TOKEN)) {
            assertThat(read(git, "bookmarks.json")).isEqualTo("{\"v\":1}");
        }
    }

    @Test
    public void testClone_reflectsRestUpdate() throws Exception {
        GistResponse gist = apiClient.createGist("test", "bookmarks.json", "{\"v\":1}");
        apiClient.updateGist(gist.id, "bookmarks.json", "{\"v\":2}", null);

        try (Git git = clone(gist, FakeGistApiServer.TOKEN)) {
            assertThat(read(git, "bookmarks.json")).isEqualTo("{\"v\":2}");
        }
    }

    @Test
    public void testPush_updatesRestView() throws Exception {
        GistResponse gist = apiClient.createGist("test", "bookmarks.json", "{\"v\":1}");
        String etagBefore = apiClient.conditionalGetEtag(gist.id, null);

        try (Git git = clone(gist, FakeGistApiServer.TOKEN)) {
            Files.writeString(workTree(git).resolve("bookmarks.json"), "{\"v\":2}");
            git.commit().setAll(true).setMessage("").call();
            Iterable<PushResult> results = git.push().setCredentialsProvider(credentials(FakeGistApiServer.TOKEN)).call();
            for (PushResult result : results) {
                for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                    assertThat(update.getStatus()).isEqualTo(RemoteRefUpdate.Status.OK);
                }
            }
        }

        GistResponse loaded = apiClient.loadGist(gist.id);
        assertThat(loaded.files.get("bookmarks.json").content).isEqualTo("{\"v\":2}");
        assertThat(apiClient.conditionalGetEtag(gist.id, etagBefore)).isNotNull().isNotEqualTo(etagBefore);
    }

    @Test
    public void testClone_withWrongToken_isRejected() throws Exception {
        GistResponse gist = apiClient.createGist("test", "bookmarks.json", "{}");

        assertThatThrownBy(() -> clone(gist, "wrong-token")).isInstanceOf(TransportException.class);
    }

    private Git clone(GistResponse gist, String token) throws Exception {
        return Git.cloneRepository()
                .setURI(gist.git_pull_url)
                .setDirectory(tempFolder.newFolder())
                .setCredentialsProvider(credentials(token))
                .call();
    }

    private static UsernamePasswordCredentialsProvider credentials(String token) {
        return new UsernamePasswordCredentialsProvider("x-access-token", token);
    }

    private static Path workTree(Git git) {
        return git.getRepository().getWorkTree().toPath();
    }

    private static String read(Git git, String fileName) throws Exception {
        return Files.readString(workTree(git).resolve(fileName));
    }
}
