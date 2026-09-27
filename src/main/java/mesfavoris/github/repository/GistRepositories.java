package mesfavoris.github.repository;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.NioFiles;
import com.intellij.serviceContainer.NonInjectable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** Local gist clones, cached in the IDE system directory and shared by all projects (one lock per gist). */
@Service(Service.Level.APP)
public final class GistRepositories {
    private static final Logger LOG = Logger.getInstance(GistRepositories.class);
    /** Gist ids are hexadecimal (or decimal for very old gists): never a path that could leave the base directory. */
    private static final Pattern GIST_ID = Pattern.compile("[0-9a-fA-F]+");

    private final Path baseDirectory;
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public GistRepositories() {
        this(Path.of(PathManager.getSystemPath(), "mesfavoris", "gists"));
    }

    @NonInjectable
    GistRepositories(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public static GistRepositories getInstance() {
        return ApplicationManager.getApplication().getService(GistRepositories.class);
    }

    /**
     * @param gitUrlSupplier gives the gist's git url when the clone has to be created; once cloned, the url
     *                       recorded in the clone is used
     */
    public GistRepository getRepository(Project project, String gistId,
                                        ThrowableComputable<String, IOException> gitUrlSupplier,
                                        Supplier<String> tokenSupplier, @Nullable String userLogin)
            throws IOException {
        return new GistRepository(project, getDirectory(gistId), lock(gistId), gitUrlSupplier, tokenSupplier,
                userLogin);
    }

    public Path getDirectory(String gistId) throws IOException {
        // gist ids come from project files that may be shared: never trust them as paths
        if (!GIST_ID.matcher(gistId).matches()) {
            throw new IOException("Invalid gist id: " + gistId);
        }
        return baseDirectory.resolve(gistId);
    }

    public void delete(String gistId) {
        ReentrantLock lock = lock(gistId);
        lock.lock();
        try {
            NioFiles.deleteRecursively(getDirectory(gistId));
        } catch (IOException e) {
            LOG.warn("Could not delete local clone of gist " + gistId, e);
        } finally {
            lock.unlock();
        }
    }

    private ReentrantLock lock(String gistId) {
        return locks.computeIfAbsent(gistId, id -> new ReentrantLock());
    }
}
