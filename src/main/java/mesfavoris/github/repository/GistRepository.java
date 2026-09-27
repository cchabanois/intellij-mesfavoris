package mesfavoris.github.repository;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.NioFiles;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import mesfavoris.remote.ConflictException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Local clone of a gist: loads with fetch and saves with commit + push, so only the delta goes over the network.
 * The clone is shallow when created, and re-created when missing or broken.
 */
public class GistRepository {
    private static final String CLONE_DIR_NAME = "repo";
    private static final String CLONE_SUFFIX = ".clone-";

    private final Project project;
    private final Path directory;
    private final Lock lock;
    private final ThrowableComputable<String, IOException> gitUrlSupplier;
    private final Supplier<String> tokenSupplier;
    @Nullable
    private final String userLogin;

    /** The content of a gist file at {@code commitId}. */
    public record Snapshot(String commitId, byte[] content) {
    }

    GistRepository(Project project, Path directory, Lock lock,
                   ThrowableComputable<String, IOException> gitUrlSupplier, Supplier<String> tokenSupplier,
                   @Nullable String userLogin) {
        this.project = project;
        this.directory = directory;
        this.lock = lock;
        this.gitUrlSupplier = gitUrlSupplier;
        this.tokenSupplier = tokenSupplier;
        this.userLogin = userLogin;
    }

    /** Brings the clone to the latest version of the gist and returns the content of {@code fileName}. */
    public Snapshot pull(String fileName) throws IOException {
        lock.lock();
        try {
            update();
            return new Snapshot(head(), readFile(fileName));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Commits {@code content} as {@code fileName} on top of {@code expectedCommitId} and pushes it.
     *
     * @param expectedCommitId the commit the content is based on, or null to save on top of the latest version
     * @throws ConflictException if the gist has moved since {@code expectedCommitId}
     */
    public String commitAndPush(String fileName, byte[] content, @Nullable String expectedCommitId)
            throws IOException, ConflictException {
        lock.lock();
        try {
            if (expectedCommitId == null) {
                update();
                expectedCommitId = head();
            } else {
                ensureCloned();
            }
            String head = head();
            if (!head.equals(expectedCommitId)) {
                throw new ConflictException();
            }
            Path file = directory.resolve(fileName);
            checkNotSymbolicLink(file);
            if (Files.isRegularFile(file) && Arrays.equals(Files.readAllBytes(file), content)) {
                return head;
            }
            try {
                Files.write(file, content);
                runOrThrow(GitCommand.ADD, null, List.of(fileName));
                commit("Update " + fileName);
                push();
                return head();
            } catch (IOException | ConflictException e) {
                // drop the local commit so the clone keeps matching the remote
                run(GitCommand.RESET, null, List.of("--hard", head), Map.of());
                throw e;
            }
        } finally {
            lock.unlock();
        }
    }

    private void commit(String message) throws IOException {
        String name = userLogin != null ? userLogin : "mesfavoris";
        String email = name + "@users.noreply.github.com";
        GitCommandResult result = run(GitCommand.COMMIT, null,
                List.of("--no-verify", "--no-gpg-sign", "-m", message),
                Map.of("GIT_AUTHOR_NAME", name, "GIT_AUTHOR_EMAIL", email,
                        "GIT_COMMITTER_NAME", name, "GIT_COMMITTER_EMAIL", email));
        check(result, "commit");
    }

    private void push() throws IOException, ConflictException {
        String url = authenticatedUrl();
        GitCommandResult result = run(GitCommand.PUSH, url,
                List.of("--no-verify", url, "HEAD:refs/heads/" + currentBranch()), Map.of());
        if (result.success()) {
            return;
        }
        // "[rejected]" = not a fast-forward: the gist moved; "[remote rejected]" (e.g. no permission) is an error
        if ((result.getErrorOutputAsJoinedString() + result.getOutputAsJoinedString()).contains("[rejected]")) {
            throw new ConflictException();
        }
        check(result, "push");
    }

    /** Brings the clone to the latest version of the gist. */
    private void update() throws IOException {
        if (ensureCloned()) {
            return;
        }
        String branch = currentBranch();
        String url = authenticatedUrl();
        runOrThrow(GitCommand.FETCH, url, List.of(url, "+refs/heads/" + branch + ":refs/remotes/origin/" + branch));
        runOrThrow(GitCommand.RESET, null, List.of("--hard", "refs/remotes/origin/" + branch));
    }

    /** Returns true if the clone has just been created, and is thus up to date. */
    private boolean ensureCloned() throws IOException {
        Path gitDirectory = directory.resolve(".git");
        if (Files.isDirectory(gitDirectory)) {
            deleteStaleLockFiles(gitDirectory);
            if (revParse("--verify", "HEAD") != null) {
                return false;
            }
        }
        if (Files.exists(directory)) {
            NioFiles.deleteRecursively(directory);
        }
        Files.createDirectories(directory.getParent());
        deleteStaleCloneDirectories();
        // clone next to the final location then move it, so an interrupted clone never looks valid
        Path tempParent = Files.createTempDirectory(directory.getParent(), directory.getFileName() + CLONE_SUFFIX);
        try {
            String gitUrl = gitUrlSupplier.compute();
            if (gitUrl == null) {
                throw new IOException("No git url for gist " + directory.getFileName());
            }
            String url = authenticatedGitUrl(gitUrl, tokenSupplier.get());
            GitCommandResult result = Git.getInstance().runCommand(() -> {
                GitLineHandler handler = new GitLineHandler(project, tempParent.toFile(), GitCommand.CLONE);
                handler.setUrl(url); // masks the embedded token in logged commands
                // no symlinks: a gist file linking outside the clone must never be read or written through
                handler.addParameters("--depth=1", "--config", "core.autocrlf=false",
                        "--config", "core.symlinks=false", "--config", "commit.gpgsign=false", url);
                handler.endOptions();
                handler.addParameters(CLONE_DIR_NAME);
                return handler;
            });
            check(result, "clone");
            Path clone = tempParent.resolve(CLONE_DIR_NAME);
            // keep the token out of .git/config: fetch and push pass the authenticated URL explicitly
            GitCommandResult setUrl = run(clone, GitCommand.REMOTE, null, List.of("set-url", "origin", gitUrl), Map.of());
            check(setUrl, "remote set-url");
            Files.move(clone, directory, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            try {
                NioFiles.deleteRecursively(tempParent);
            } catch (IOException e) {
                // leftovers are deleted before the next clone
            }
        }
        return true;
    }

    /** Lock files left by a killed git (we hold the gist lock, so no git of ours is running on it). */
    private static void deleteStaleLockFiles(Path gitDirectory) throws IOException {
        try (Stream<Path> files = Files.walk(gitDirectory)) {
            for (Path lockFile : files.filter(file -> file.getFileName().toString().endsWith(".lock")).toList()) {
                Files.deleteIfExists(lockFile);
            }
        }
    }

    /** Temporary clones left when the IDE was killed during a clone. */
    private void deleteStaleCloneDirectories() throws IOException {
        try (DirectoryStream<Path> stale = Files.newDirectoryStream(directory.getParent(),
                directory.getFileName() + CLONE_SUFFIX + "*")) {
            for (Path cloneDirectory : stale) {
                NioFiles.deleteRecursively(cloneDirectory);
            }
        }
    }

    private byte[] readFile(String fileName) throws IOException {
        Path file = directory.resolve(fileName);
        checkNotSymbolicLink(file);
        if (!Files.isRegularFile(file)) {
            throw new IOException("Gist " + directory.getFileName() + " does not contain " + fileName);
        }
        return Files.readAllBytes(file);
    }

    private static void checkNotSymbolicLink(Path file) throws IOException {
        if (Files.isSymbolicLink(file)) {
            throw new IOException(file.getFileName() + " is a symbolic link");
        }
    }

    private String head() throws IOException {
        String head = revParse("HEAD");
        if (head == null) {
            throw new IOException("Could not read the HEAD of gist " + directory.getFileName());
        }
        return head;
    }

    private String currentBranch() throws IOException {
        // old gists use "master", recent ones "main"
        String branch = revParse("--abbrev-ref", "HEAD");
        if (branch == null) {
            throw new IOException("Could not read the branch of gist " + directory.getFileName());
        }
        return branch;
    }

    @Nullable
    private String revParse(String... params) {
        GitCommandResult result = run(GitCommand.REV_PARSE, null, List.of(params), Map.of());
        return result.success() ? result.getOutputAsJoinedString().trim() : null;
    }

    private void runOrThrow(GitCommand command, @Nullable String url, List<String> params) throws IOException {
        check(run(command, url, params, Map.of()), command.toString());
    }

    private GitCommandResult run(GitCommand command, @Nullable String url, List<String> params,
                                 Map<String, String> environment) {
        return run(directory, command, url, params, environment);
    }

    private GitCommandResult run(Path workingDirectory, GitCommand command, @Nullable String url,
                                 List<String> params, Map<String, String> environment) {
        return Git.getInstance().runCommand(() -> {
            GitLineHandler handler = new GitLineHandler(project, workingDirectory.toFile(), command);
            if (url != null) {
                handler.setUrl(url);
            }
            environment.forEach(handler::addCustomEnvironmentVariable);
            handler.addParameters(params);
            return handler;
        });
    }

    private static void check(GitCommandResult result, String what) throws IOException {
        if (!result.success()) {
            // git strips the userinfo (token) from URLs in its stderr, so this is safe to surface
            throw new IOException("git " + what + " failed for gist: " + result.getErrorOutputAsJoinedString());
        }
    }

    /** The url the clone was made from: it lives in the IDE system directory, unlike shareable project files. */
    private String authenticatedUrl() throws IOException {
        GitCommandResult result = run(GitCommand.REMOTE, null, List.of("get-url", "origin"), Map.of());
        check(result, "remote get-url");
        return authenticatedGitUrl(result.getOutputAsJoinedString().trim(), tokenSupplier.get());
    }

    /** Embeds the token as {@code https://x-access-token:<token>@host/...}; package-private for unit tests. */
    static String authenticatedGitUrl(String gitUrl, String token) throws IOException {
        URI uri = URI.create(gitUrl);
        try {
            return new URI(uri.getScheme(), "x-access-token:" + token, uri.getHost(), uri.getPort(),
                    uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
        } catch (URISyntaxException e) {
            throw new IOException("Invalid gist git url: " + gitUrl, e);
        }
    }
}
