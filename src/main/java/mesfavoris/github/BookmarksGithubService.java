package mesfavoris.github;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import mesfavoris.github.connection.GithubConnectionManager;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
/* Project service that lazily creates and holds the {@link GithubConnectionManager}. */
public final class BookmarksGithubService {
    private final Project project;
    private GithubConnectionManager connectionManager;

    public BookmarksGithubService(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public synchronized GithubConnectionManager getConnectionManager() {
        if (connectionManager == null) {
            connectionManager = new GithubConnectionManager(project);
            connectionManager.init();
        }
        return connectionManager;
    }
}
