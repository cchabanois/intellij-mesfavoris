package mesfavoris.github.client.content;
import mesfavoris.github.client.IGistFileContentProvider;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.NioFiles;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import mesfavoris.github.client.GistFile;
import mesfavoris.github.client.GistResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Fetches file content by cloning the Gist git repository ({@code git_pull_url}) with git4idea's {@link Git}
 * service, so the clone uses the same git executable, process handling and progress reporting as the rest of
 * the IDE. This is the reliable fallback when the raw URL cannot serve the content — files larger than
 * 10&nbsp;MB (not served as raw) or hosts that redirect raw requests to a login page. Authentication is
 * supplied by embedding the OAuth token in the clone URL; the clone lands in a throwaway temp directory that
 * is always deleted.
 */
public class CloneGistFileContentProvider implements IGistFileContentProvider {
    private static final String CLONE_DIR_NAME = "gist";

    private final Project project;
    private final Supplier<String> tokenSupplier;

    public CloneGistFileContentProvider(Project project, Supplier<String> tokenSupplier) {
        this.project = project;
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public byte[] getFileContent(GistResponse gist, GistFile file) throws IOException {
        if (gist.git_pull_url == null) {
            throw new IOException("Gist " + gist.id + " has no git pull url");
        }
        Path tempParent = Files.createTempDirectory("mesfavoris-gist-");
        try {
            String url = authenticatedGitUrl(gist.git_pull_url, tokenSupplier.get());
            GitCommandResult result = Git.getInstance()
                    .clone(project, tempParent.toFile(), url, CLONE_DIR_NAME);
            if (!result.success()) {
                // git strips the userinfo (token) from the URL in its stderr, so this is safe to surface as-is.
                throw new IOException("Failed to clone gist: " + result.getErrorOutputAsJoinedString());
            }
            Path clonedFile = tempParent.resolve(CLONE_DIR_NAME).resolve(file.filename);
            if (!Files.isRegularFile(clonedFile)) {
                throw new IOException("Cloned gist does not contain " + file.filename);
            }
            return Files.readAllBytes(clonedFile);
        } finally {
            // Best-effort cleanup: deleteQuietly won't throw out of the finally and mask a clone failure.
            NioFiles.deleteQuietly(tempParent);
        }
    }

    /**
     * Embeds the OAuth token in the clone URL as {@code https://x-access-token:<token>@host/...}.
     * Package-private for unit testing; the only public entry point ({@link #getFileContent}) requires a
     * running IDE to clone.
     */
    static String authenticatedGitUrl(String gitPullUrl, String token) throws IOException {
        URI uri = URI.create(gitPullUrl);
        try {
            return new URI(uri.getScheme(), "x-access-token:" + token, uri.getHost(), uri.getPort(),
                    uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
        } catch (URISyntaxException e) {
            throw new IOException("Invalid gist git url: " + gitPullUrl, e);
        }
    }
}
