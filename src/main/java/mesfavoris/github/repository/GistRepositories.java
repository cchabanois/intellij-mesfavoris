package mesfavoris.github.repository;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.serviceContainer.NonInjectable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** Local gist clones, cached in the IDE system directory and shared by all projects (one {@link GistClone} per gist). */
@Service(Service.Level.APP)
public final class GistRepositories {
    private static final Logger LOG = Logger.getInstance(GistRepositories.class);
    /** Gist ids are hexadecimal (or decimal for very old gists): never a path that could leave the base directory. */
    private static final Pattern GIST_ID = Pattern.compile("[0-9a-fA-F]+");

    private final Path baseDirectory;
    private final Map<String, GistClone> clones = new ConcurrentHashMap<>();

    public GistRepositories() {
        this(Path.of(PathManager.getSystemPath(), "mesfavoris", "gists"));
    }

    @NonInjectable
    public GistRepositories(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public GistRepository getRepository(Project project, String gistId, String gitUrl,
                                        Supplier<String> tokenSupplier, @Nullable String userLogin)
            throws IOException {
        return new GistRepository(project, getClone(gistId), gitUrl, tokenSupplier, userLogin);
    }

    /** The git url the gist's local clone was made from, or null if it has not been cloned yet. */
    @Nullable
    public String getClonedGitUrl(Project project, String gistId) throws IOException {
        return GistRepository.readClonedGitUrl(project, getDirectory(gistId));
    }

    public Path getDirectory(String gistId) throws IOException {
        return getClone(gistId).getDirectory();
    }

    public void delete(String gistId) {
        try {
            getClone(gistId).delete();
        } catch (IOException e) {
            LOG.warn("Could not delete local clone of gist " + gistId, e);
        }
    }

    private GistClone getClone(String gistId) throws IOException {
        // gist ids come from project files that may be shared: never trust them as paths
        if (!GIST_ID.matcher(gistId).matches()) {
            throw new IOException("Invalid gist id: " + gistId);
        }
        return clones.computeIfAbsent(gistId, id -> new GistClone(baseDirectory.resolve(id)));
    }
}
