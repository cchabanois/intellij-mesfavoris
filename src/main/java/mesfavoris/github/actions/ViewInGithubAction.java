package mesfavoris.github.actions;

import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import mesfavoris.github.mappings.GistMapping;
import mesfavoris.github.mappings.GistMappingsStore;
import mesfavoris.internal.actions.AbstractBookmarkAction;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

public class ViewInGithubAction extends AbstractBookmarkAction {
    private static final Logger LOG = Logger.getInstance(ViewInGithubAction.class);

    public ViewInGithubAction() {
        super();
        getTemplatePresentation().setText("View in GitHub");
        getTemplatePresentation().setDescription("Open the bookmark folder in GitHub Gists");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(getGistUrl(event).isPresent());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        getGistUrl(event).ifPresent(url -> {
            try {
                BrowserLauncher.getInstance().browse(new URI(url));
            } catch (URISyntaxException e) {
                LOG.error("Could not open browser", e);
            }
        });
    }

    private Optional<String> getGistUrl(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return Optional.empty();
        BookmarkFolder folder = getSelectedBookmarkFolder(event);
        if (folder == null) return Optional.empty();
        return project.getService(GistMappingsStore.class)
                .getMapping(folder.getId())
                .map(m -> m.getProperties().get(GistMapping.PROP_GIST_URL));
    }

    private BookmarkFolder getSelectedBookmarkFolder(@NotNull AnActionEvent event) {
        List<Bookmark> selected = getSelectedBookmarks(event);
        if (selected.size() != 1 || !(selected.getFirst() instanceof BookmarkFolder folder)) return null;
        return folder;
    }
}
