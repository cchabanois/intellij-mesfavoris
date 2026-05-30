package mesfavoris.github.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import mesfavoris.github.mappings.GistMapping;
import mesfavoris.github.mappings.GistMappingsStore;
import mesfavoris.internal.actions.AbstractBookmarkAction;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Optional;

public class CopyGistLinkAction extends AbstractBookmarkAction {

    public CopyGistLinkAction() {
        super();
        getTemplatePresentation().setText("Copy Shareable Link");
        getTemplatePresentation().setDescription("Copy the GitHub Gist URL to the clipboard");
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
            CopyPasteManager.getInstance().setContents(new StringSelection(url));
            Notification notification = new Notification(
                    "com.cchabanois.mesfavoris.info",
                    "Gist link copied to clipboard",
                    "The GitHub Gist URL has been copied to your clipboard",
                    NotificationType.INFORMATION
            );
            Notifications.Bus.notify(notification, event.getProject());
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
