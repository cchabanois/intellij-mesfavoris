package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

public class AddBookmarkAction extends AbstractAddBookmarkAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        BookmarksService bookmarksService = getBookmarksService(event);
        DataContext dataContext = event.getDataContext();
        runAddBookmarkTask(event, dataContext);
    }
}
