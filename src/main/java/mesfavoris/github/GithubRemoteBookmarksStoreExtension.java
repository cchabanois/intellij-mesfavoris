package mesfavoris.github;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import mesfavoris.github.actions.ImportBookmarksFromGithubAction;
import mesfavoris.github.actions.ViewInGithubAction;
import mesfavoris.github.changes.GistChangeManager;
import mesfavoris.github.connection.GithubConnectionManager;
import mesfavoris.github.mappings.GistMappingsStore;
import mesfavoris.remote.AbstractRemoteBookmarksStore;
import mesfavoris.remote.AbstractRemoteBookmarksStoreExtension;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/** Registers the GitHub Gists remote store under the id {@code "github"}. */
public class GithubRemoteBookmarksStoreExtension extends AbstractRemoteBookmarksStoreExtension {
    private static final String ID = "github";
    private static final String LABEL = "GitHub Gists";
    public static final String USER_AGENT = "mesfavoris-intellij-plugin";

    public GithubRemoteBookmarksStoreExtension() {
        super(ID, LABEL, GithubIcons.GITHUB, GithubIcons.GITHUB_OVERLAY);
    }

    @NotNull
    @Override
    public AbstractRemoteBookmarksStore createStore(@NotNull Project project) {
        BookmarksGithubService service = project.getService(BookmarksGithubService.class);
        GithubConnectionManager connectionManager = service.getConnectionManager();
        GistMappingsStore mappingsStore = project.getService(GistMappingsStore.class);
        ScheduledExecutorService executor = AppExecutorUtil.getAppScheduledExecutorService();
        GistChangeManager changeManager = new GistChangeManager(project, connectionManager, mappingsStore, executor);
        return new GithubRemoteBookmarksStore(project, connectionManager, mappingsStore, changeManager);
    }

    @NotNull
    @Override
    public List<AnAction> getAdditionalActions() {
        return List.of(new ImportBookmarksFromGithubAction(), new ViewInGithubAction());
    }
}
