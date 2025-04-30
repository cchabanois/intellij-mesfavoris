package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import mesfavoris.BookmarksException;
import mesfavoris.model.Bookmark;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

public class GotoBookmarkAction extends AnAction  {

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
        Project project = event.getProject();
        ProgressManager.getInstance().run(new Task.Modal(event.getProject(), "Goto Bookmark", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    BookmarksService bookmarksService = project.getService(BookmarksService.class);
                    bookmarksService.gotoBookmark(bookmark.getId(), indicator);
                } catch (BookmarksException e) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showMessageDialog(e.getMessage(), "Could not goto bookmark", Messages.getInformationIcon()));
                }
            }
        });
    }

    private Bookmark getSelectedBookmark(AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        Object object = dataContext.getData(PlatformDataKeys.SELECTED_ITEM);
        return object instanceof Bookmark bookmark ? bookmark : null;
    }

}
