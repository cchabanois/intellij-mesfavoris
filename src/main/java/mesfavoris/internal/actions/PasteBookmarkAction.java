package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.ide.CopyPasteManager;
import mesfavoris.BookmarksDataKeys;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.Transferable;

public class PasteBookmarkAction extends AbstractAddBookmarkAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        // Only enable when the bookmarks tree has focus
        if (!isFocusOnBookmarksTree()) {
            event.getPresentation().setEnabled(false);
            return;
        }

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
        runAddBookmarkTask(event, newDataContext);
    }
}