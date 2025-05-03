package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import mesfavoris.BookmarksException;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

public class AddBookmarkAction extends AbstractBookmarkAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        BookmarksService bookmarksService = getBookmarksService(event);
        DataContext dataContext = event.getDataContext();

        ProgressManager.getInstance().run(new Task.Modal(event.getProject(), "Adding Bookmark", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    bookmarksService.addBookmark(dataContext, indicator);
                } catch (BookmarksException e) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showMessageDialog(e.getMessage(), "Could not add bookmark", Messages.getInformationIcon()));
                }
            }
        });
    }
}
