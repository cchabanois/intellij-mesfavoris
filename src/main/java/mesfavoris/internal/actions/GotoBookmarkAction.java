package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import mesfavoris.BookmarksException;
import mesfavoris.model.Bookmark;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

public class GotoBookmarkAction extends AbstractBookmarkAction  {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Bookmark bookmark = getSelectedBookmark(event);
        event.getPresentation().setEnabledAndVisible(bookmark != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Bookmark bookmark = getSelectedBookmark(event);
        if (bookmark == null) {
            return;
        }
        IBookmarksService bookmarksService = getBookmarksService(event);
        ProgressManager.getInstance().run(new Task.Modal(event.getProject(), "Goto Bookmark", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    bookmarksService.gotoBookmark(bookmark.getId(), indicator);
                } catch (BookmarksException e) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showMessageDialog(e.getMessage(), "Could not goto bookmark", Messages.getInformationIcon()));
                }
            }
        });
    }

}
