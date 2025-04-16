package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import mesfavoris.BookmarksException;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

public class AddBookmarkAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        BookmarksService bookmarksService = project.getService(BookmarksService.class);
        DataContext dataContext = event.getDataContext();
        ProgressIndicator progress = EmptyProgressIndicator.notNullize(ProgressIndicatorProvider.getGlobalProgressIndicator());
        try {
            bookmarksService.addBookmark(dataContext, progress);
        } catch (BookmarksException e) {
            Messages.showMessageDialog(e.getMessage(), "Could not add bookmark", Messages.getInformationIcon());
        }
    }
}
