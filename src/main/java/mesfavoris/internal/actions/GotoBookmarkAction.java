package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
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
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        BookmarksService bookmarksService = project.getService(BookmarksService.class);
        DataContext dataContext = event.getDataContext();
        Bookmark bookmark = (Bookmark) dataContext.getData(PlatformDataKeys.SELECTED_ITEM);
        if (bookmark == null) {
            return;
        }
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
