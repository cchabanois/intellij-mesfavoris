package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.ide.CopyPasteManager;
import mesfavoris.BookmarksDataKeys;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

public class PasteAsSnippetAction extends AbstractAddBookmarkAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        Transferable transferable = CopyPasteManager.getInstance().getContents();
        event.getPresentation().setEnabled(transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Transferable transferable = CopyPasteManager.getInstance().getContents();
        if (transferable == null || !transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return;
        }
        BookmarksService bookmarksService = getBookmarksService(event);
        DataContext originalContext = event.getDataContext();
        DataContext newDataContext = SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, event.getProject())
                .add(BookmarksDataKeys.TRANSFERABLE_DATA_KEY, transferable)
                .add(BookmarksDataKeys.BOOKMARK_TYPE_DATA_KEY, "snippet")
                .build();
        runAddBookmarkTask(event, newDataContext);
    }
}
