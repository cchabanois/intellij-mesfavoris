package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import mesfavoris.BookmarksException;
import mesfavoris.internal.service.operations.CutBookmarkOperation;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class CutBookmarkAction extends AbstractBookmarkAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        List<Bookmark> bookmarks = getSelectedBookmarks(event);
        boolean hasBookmarks = !bookmarks.isEmpty();

        // Only enable the action if focus is on the bookmarks tree
        boolean focusOnTree = isFocusOnBookmarksTree();

        if (!hasBookmarks || !focusOnTree) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }

        // Check if there are duplicated bookmarks in selection
        IBookmarksService bookmarksService = getBookmarksService(event);
        List<BookmarkId> bookmarkIds = bookmarks.stream()
                .map(Bookmark::getId)
                .collect(Collectors.toList());

        CutBookmarkOperation operation = new CutBookmarkOperation(bookmarksService.getBookmarkDatabase());
        boolean hasDuplicates = operation.hasDuplicatedBookmarksInSelection(
                bookmarksService.getBookmarksTree(), bookmarkIds);

        event.getPresentation().setEnabledAndVisible(!hasDuplicates);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        List<Bookmark> bookmarks = getSelectedBookmarks(event);
        if (bookmarks.isEmpty()) {
            return;
        }

        IBookmarksService bookmarksService = getBookmarksService(event);
        List<BookmarkId> bookmarkIds = bookmarks.stream()
                .map(Bookmark::getId)
                .collect(Collectors.toList());

        try {
            CutBookmarkOperation operation = new CutBookmarkOperation(bookmarksService.getBookmarkDatabase());
            operation.cutToClipboard(bookmarkIds);
        } catch (BookmarksException e) {
            ApplicationManager.getApplication().invokeLater(() ->
                Messages.showMessageDialog(e.getMessage(), "Could not cut bookmarks", Messages.getErrorIcon()));
        }
    }
}

