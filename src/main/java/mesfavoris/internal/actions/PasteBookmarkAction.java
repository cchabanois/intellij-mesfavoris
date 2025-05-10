package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import mesfavoris.BookmarksDataKeys;
import mesfavoris.BookmarksException;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.Transferable;

public class PasteBookmarkAction extends AbstractBookmarkAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        Transferable transferable = CopyPasteManager.getInstance().getContents();
        event.getPresentation().setEnabled(transferable != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Transferable transferable = CopyPasteManager.getInstance().getContents();
        if (transferable == null) {
            return;
        }
        BookmarksService bookmarksService = getBookmarksService(event);
        DataContext originalContext = event.getDataContext();
        DataContext newDataContext = SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, event.getProject())
                .add(BookmarksDataKeys.TRANSFERABLE_DATA_KEY, transferable)
                .build();
        ProgressManager.getInstance().run(new Task.Modal(event.getProject(), "Adding Bookmark", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    bookmarksService.addBookmark(newDataContext, indicator);
                } catch (BookmarksException e) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showMessageDialog(e.getMessage(), "Could not add bookmark", Messages.getInformationIcon()));
                }
            }
        });

    }
}