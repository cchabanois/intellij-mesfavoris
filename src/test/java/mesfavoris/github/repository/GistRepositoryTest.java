package mesfavoris.github.repository;

import com.intellij.openapi.util.io.NioFiles;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.github.GithubTestUser;
import mesfavoris.github.client.GistApiClient;
import mesfavoris.github.client.GistResponse;
import mesfavoris.github.test.FakeGistApiServer;
import mesfavoris.remote.ConflictException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs real git commands (needs a {@code git} executable) against the real GitHub when {@code USER1_GITHUB_TOKEN}
 * is set, otherwise against {@link FakeGistApiServer}.
 */
public class GistRepositoryTest extends BasePlatformTestCase {

    private static final String FILE_NAME = "data.json";

    private FakeGistApiServer fakeServer;
    private GistApiClient apiClient;
    private String token;
    private Path baseDirectory;
    private GistRepositories repositories;
    private final List<String> createdGistIds = new ArrayList<>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Optional<String> realToken = GithubTestUser.USER1.getToken();
        if (realToken.isPresent()) {
            token = realToken.get();
            String apiBaseUrl = GithubTestUser.USER1.getApiBaseUrl();
            apiClient = new GistApiClient(() -> token, () -> apiBaseUrl, GistApiClient.newHttpClient());
        } else {
            fakeServer = new FakeGistApiServer();
            fakeServer.start();
            token = FakeGistApiServer.TOKEN;
            apiClient = fakeServer.newApiClient();
        }
        baseDirectory = Files.createTempDirectory("gist-repositories-");
        repositories = new GistRepositories(baseDirectory);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            for (String gistId : createdGistIds) {
                apiClient.deleteGist(gistId);
            }
            if (fakeServer != null) {
                fakeServer.stop();
            }
            NioFiles.deleteRecursively(baseDirectory);
        } finally {
            super.tearDown();
        }
    }

    public void testPull_clonesGist() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{\"v\":1}");

        GistRepository.Snapshot snapshot = repository(gist).pull(FILE_NAME);

        assertThat(string(snapshot.content())).isEqualTo("{\"v\":1}");
        assertThat(snapshot.commitId()).isEqualTo(gist.latestVersion());
        assertThat(repositories.getDirectory(gist.id).resolve(".git")).isDirectory();
    }

    public void testPull_returnsRemoteChanges() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{\"v\":1}");
        GistRepository repository = repository(gist);
        String firstCommitId = repository.pull(FILE_NAME).commitId();
        apiClient.updateGist(gist.id, FILE_NAME, "{\"v\":2}", null);

        GistRepository.Snapshot snapshot = repository.pull(FILE_NAME);

        assertThat(string(snapshot.content())).isEqualTo("{\"v\":2}");
        assertThat(snapshot.commitId()).isNotEqualTo(firstCommitId);
    }

    public void testPull_recreatesDeletedClone() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{\"v\":1}");
        repository(gist).pull(FILE_NAME);
        NioFiles.deleteRecursively(repositories.getDirectory(gist.id));

        GistRepository.Snapshot snapshot = repository(gist).pull(FILE_NAME);

        assertThat(string(snapshot.content())).isEqualTo("{\"v\":1}");
    }

    public void testPull_recreatesBrokenClone() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{\"v\":1}");
        repository(gist).pull(FILE_NAME);
        Files.delete(repositories.getDirectory(gist.id).resolve(".git").resolve("HEAD"));

        GistRepository.Snapshot snapshot = repository(gist).pull(FILE_NAME);

        assertThat(string(snapshot.content())).isEqualTo("{\"v\":1}");
    }

    public void testCommitAndPush_afterGitWasKilled_ignoresStaleLockFiles() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{\"v\":1}");
        GistRepository repository = repository(gist);
        String commitId = repository.pull(FILE_NAME).commitId();
        Path gitDirectory = repositories.getDirectory(gist.id).resolve(".git");
        Files.createFile(gitDirectory.resolve("index.lock"));
        Files.createFile(gitDirectory.resolve("HEAD.lock"));

        repository.commitAndPush(FILE_NAME, bytes("{\"v\":2}"), commitId);

        assertThat(restContent(gist.id)).isEqualTo("{\"v\":2}");
    }

    public void testPull_deletesTemporaryClonesLeftByKilledClone() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{\"v\":1}");
        Path staleClone = Files.createDirectories(baseDirectory.resolve(gist.id + ".clone-123").resolve("repo"));

        repository(gist).pull(FILE_NAME);

        assertThat(staleClone.getParent()).doesNotExist();
        assertThat(repositories.getDirectory(gist.id).resolve(".git")).isDirectory();
    }

    public void testPull_missingFile_throwsIOException() throws Exception {
        GistResponse gist = createGist("other.json", "{}");

        assertThatThrownBy(() -> repository(gist).pull(FILE_NAME))
                .isInstanceOf(IOException.class)
                .hasMessageContaining(FILE_NAME);
    }

    public void testPull_doesNotStoreTokenInClone() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{}");

        repository(gist).pull(FILE_NAME);

        String config = Files.readString(repositories.getDirectory(gist.id).resolve(".git").resolve("config"));
        assertThat(config).doesNotContain(token);
    }

    public void testCommitAndPush_updatesGist() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{\"v\":1}");
        GistRepository repository = repository(gist);
        String commitId = repository.pull(FILE_NAME).commitId();

        String newCommitId = repository.commitAndPush(FILE_NAME, bytes("{\"v\":2}"), commitId);

        assertThat(newCommitId).isNotEqualTo(commitId);
        GistResponse loaded = apiClient.loadGist(gist.id);
        assertThat(loaded.files.get(FILE_NAME).content).isEqualTo("{\"v\":2}");
        assertThat(loaded.latestVersion()).isEqualTo(newCommitId);
    }

    public void testCommitAndPush_unchangedContent_createsNoCommit() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{\"v\":1}");
        GistRepository repository = repository(gist);
        String commitId = repository.pull(FILE_NAME).commitId();

        assertThat(repository.commitAndPush(FILE_NAME, bytes("{\"v\":1}"), commitId)).isEqualTo(commitId);
    }

    public void testCommitAndPush_staleExpectedCommit_throwsConflictException() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{\"v\":1}");
        GistRepository repository = repository(gist);
        String staleCommitId = repository.pull(FILE_NAME).commitId();
        apiClient.updateGist(gist.id, FILE_NAME, "{\"v\":2}", null);
        repository.pull(FILE_NAME);

        assertThatThrownBy(() -> repository.commitAndPush(FILE_NAME, bytes("{\"v\":3}"), staleCommitId))
                .isInstanceOf(ConflictException.class);
        assertThat(restContent(gist.id)).isEqualTo("{\"v\":2}");
    }

    public void testCommitAndPush_gistMovedSinceLastPull_throwsConflictExceptionAndResetsClone() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{\"v\":1}");
        GistRepository repository = repository(gist);
        String commitId = repository.pull(FILE_NAME).commitId();
        apiClient.updateGist(gist.id, FILE_NAME, "{\"v\":2}", null);

        assertThatThrownBy(() -> repository.commitAndPush(FILE_NAME, bytes("{\"v\":3}"), commitId))
                .isInstanceOf(ConflictException.class);
        assertThat(restContent(gist.id)).isEqualTo("{\"v\":2}");
        assertThat(Files.readString(repositories.getDirectory(gist.id).resolve(FILE_NAME)))
                .isEqualTo("{\"v\":1}");
    }

    public void testCommitAndPush_withoutExpectedCommit_savesOnTopOfLatestVersion() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{\"v\":1}");
        GistRepository repository = repository(gist);
        repository.pull(FILE_NAME);
        apiClient.updateGist(gist.id, FILE_NAME, "{\"v\":2}", null);

        repository.commitAndPush(FILE_NAME, bytes("{\"v\":3}"), null);

        assertThat(restContent(gist.id)).isEqualTo("{\"v\":3}");
    }

    public void testPull_existingClone_usesTheGitUrlItWasClonedFrom() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{\"v\":1}");
        AtomicInteger gitUrlRequests = new AtomicInteger();
        GistRepository repository = repositories.getRepository(getProject(), gist.id,
                () -> {
                    gitUrlRequests.incrementAndGet();
                    return gist.git_pull_url;
                }, () -> token, "test-user");
        String commitId = repository.pull(FILE_NAME).commitId();

        repository.commitAndPush(FILE_NAME, bytes("{\"v\":2}"), commitId);
        repository.pull(FILE_NAME);

        assertThat(gitUrlRequests).hasValue(1);
    }

    public void testGetRepository_invalidGistId_throwsIOException() {
        assertThatThrownBy(() -> repositories.getRepository(getProject(), "../outside",
                () -> "https://gist.github.com/abc.git", () -> token, "test-user"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid gist id");
    }

    public void testDelete_invalidGistId_deletesNothing() throws Exception {
        Path outside = Files.createTempDirectory(baseDirectory.getParent(), "outside-");
        try {
            repositories.delete("../" + outside.getFileName());

            assertThat(outside).isDirectory();
        } finally {
            NioFiles.deleteRecursively(outside);
        }
    }

    public void testPull_cloneDoesNotCheckOutSymbolicLinks() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{}");

        repository(gist).pull(FILE_NAME);

        String config = Files.readString(repositories.getDirectory(gist.id).resolve(".git").resolve("config"));
        assertThat(config).containsPattern("symlinks\\s*=\\s*false");
    }

    public void testCommitAndPush_fileReplacedBySymbolicLink_doesNotWriteThroughIt() throws Exception {
        GistResponse gist = createGist(FILE_NAME, "{\"v\":1}");
        GistRepository repository = repository(gist);
        String commitId = repository.pull(FILE_NAME).commitId();
        Path target = Files.createTempFile(baseDirectory.getParent(), "target-", ".txt");
        try {
            Files.writeString(target, "original");
            Path file = repositories.getDirectory(gist.id).resolve(FILE_NAME);
            Files.delete(file);
            try {
                Files.createSymbolicLink(file, target);
            } catch (IOException | UnsupportedOperationException e) {
                return; // no symbolic links on this platform
            }

            assertThatThrownBy(() -> repository.commitAndPush(FILE_NAME, bytes("{\"v\":2}"), commitId))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("symbolic link");
            assertThat(Files.readString(target)).isEqualTo("original");
        } finally {
            Files.deleteIfExists(target);
        }
    }

    public void testAuthenticatedGitUrl_embedsToken() throws Exception {
        assertThat(GistRepository.authenticatedGitUrl("https://gist.github.com/abc123.git", "ghp_secret"))
                .isEqualTo("https://x-access-token:ghp_secret@gist.github.com/abc123.git");
    }

    public void testAuthenticatedGitUrl_keepsPortAndPath() throws Exception {
        assertThat(GistRepository.authenticatedGitUrl("http://127.0.0.1:8080/gist/abc123.git", "tok"))
                .isEqualTo("http://x-access-token:tok@127.0.0.1:8080/gist/abc123.git");
    }

    private GistResponse createGist(String fileName, String content) throws IOException {
        GistResponse gist = apiClient.createGist("gist repository test", fileName, content);
        createdGistIds.add(gist.id);
        return gist;
    }

    private GistRepository repository(GistResponse gist) throws IOException {
        return repositories.getRepository(getProject(), gist.id, () -> gist.git_pull_url, () -> token, "test-user");
    }

    private String restContent(String gistId) throws IOException {
        return apiClient.loadGist(gistId).files.get(FILE_NAME).content;
    }

    private static byte[] bytes(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private static String string(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }
}
