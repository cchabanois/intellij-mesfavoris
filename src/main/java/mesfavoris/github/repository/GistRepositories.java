package mesfavoris.github.repository;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.NioFiles;
import com.intellij.serviceContainer.NonInjectable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** Local gist clones, cached in the IDE system directory and shared by all projects (one lock per gist). */
@Service(Service.Level.APP)
public final class GistRepositories {
    private static final Logger LOG = Logger.getInstance(GistRepositories.class);

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

    public GistRepository getRepository(Project project, String gistId, String gitUrl,
                                        Supplier<String> tokenSupplier, @Nullable String userLogin) {
        return new GistRepository(project, getDirectory(gistId), lock(gistId), gitUrl, tokenSupplier, userLogin);
    }

    public Path getDirectory(String gistId) {
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
