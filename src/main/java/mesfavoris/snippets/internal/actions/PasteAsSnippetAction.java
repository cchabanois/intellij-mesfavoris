package mesfavoris.snippets.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.ide.CopyPasteManager;
import mesfavoris.BookmarksDataKeys;
import mesfavoris.bookmarktype.IDisabledBookmarkTypesProvider;
import mesfavoris.internal.actions.AbstractAddBookmarkAction;
import mesfavoris.internal.settings.bookmarktypes.BookmarkTypesStore;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

public class PasteAsSnippetAction extends AbstractAddBookmarkAction {
    private final IDisabledBookmarkTypesProvider disabledTypesProvider;

    public PasteAsSnippetAction() {
        this(BookmarkTypesStore.getInstance());
    }

    // Constructor for testing
    public PasteAsSnippetAction(@NotNull IDisabledBookmarkTypesProvider disabledTypesProvider) {
        this.disabledTypesProvider = disabledTypesProvider;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // Check if snippet bookmark type is enabled
        if (!disabledTypesProvider.isBookmarkTypeEnabled("snippet")) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }

        // Check if we have valid transferable content
        Transferable transferable = CopyPasteManager.getInstance().getContents();
        boolean hasValidContent = transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor);

        event.getPresentation().setEnabledAndVisible(hasValidContent);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Transferable transferable = CopyPasteManager.getInstance().getContents();
        if (transferable == null || !transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return;
        }
        IBookmarksService bookmarksService = getBookmarksService(event);
        DataContext originalContext = event.getDataContext();
        DataContext newDataContext = SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, event.getProject())
                .add(BookmarksDataKeys.TRANSFERABLE_DATA_KEY, transferable)
                .add(BookmarksDataKeys.BOOKMARK_TYPE_DATA_KEY, "snippet")
                .build();
        runAddBookmarkTask(event, newDataContext);
    }
}
