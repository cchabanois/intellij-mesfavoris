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
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

public class UpdateBookmarkAction extends AbstractBookmarkAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Bookmark bookmark = getSelectedBookmark(event);
        boolean isValidBookmark = bookmark != null && !(bookmark instanceof BookmarkFolder);
        event.getPresentation().setEnabledAndVisible(isValidBookmark && isFocusOnBookmarksTree());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Bookmark bookmark = getSelectedBookmark(event);
        if (bookmark == null || bookmark instanceof BookmarkFolder) {
            return;
        }

        BookmarkId bookmarkId = bookmark.getId();
        IBookmarksService bookmarksService = getBookmarksService(event);

        ProgressManager.getInstance().run(new Task.Backgroundable(event.getProject(), "Updating Bookmark", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    bookmarksService.updateBookmark(bookmarkId, event.getDataContext(), indicator);
                } catch (BookmarksException e) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showMessageDialog(e.getMessage(), "Could Not Update Bookmark", Messages.getErrorIcon()));
                }
            }
        });
    }
}

