package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import mesfavoris.BookmarksException;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.Transferable;

public class PasteBookmarkAction extends AbstractBookmarkAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        // Only enable when the bookmarks tree has focus
        if (!isFocusOnBookmarksTree()) {
            event.getPresentation().setEnabled(false);
            return;
        }

        Bookmark selectedBookmark = getSelectedBookmark(event);
        Transferable transferable = CopyPasteManager.getInstance().getContents();
        event.getPresentation().setEnabled(selectedBookmark != null && transferable != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Bookmark selectedBookmark = getSelectedBookmark(event);
        if (selectedBookmark == null) {
            return;
        }

        Transferable transferable = CopyPasteManager.getInstance().getContents();
        if (transferable == null) {
            return;
        }

        BookmarksService bookmarksService = getBookmarksService(event);

        if (selectedBookmark instanceof BookmarkFolder) {
            paste(event, bookmarksService, selectedBookmark.getId());
        } else {
            BookmarkFolder parentFolder = bookmarksService.getBookmarksTree().getParentBookmark(selectedBookmark.getId());
            pasteAfter(event, bookmarksService, parentFolder.getId(), selectedBookmark.getId());
        }
    }

    private void paste(AnActionEvent event, BookmarksService bookmarksService, BookmarkId bookmarkFolderId) {
        ProgressManager.getInstance().run(new Task.Modal(event.getProject(), "Pasting Bookmark", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    bookmarksService.paste(bookmarkFolderId, indicator);
                } catch (BookmarksException e) {
                    ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showMessageDialog(e.getMessage(), "Could not paste", Messages.getErrorIcon()));
                }
            }
        });
    }

    private void pasteAfter(AnActionEvent event, BookmarksService bookmarksService,
                           BookmarkId parentFolderId, BookmarkId bookmarkId) {
        ProgressManager.getInstance().run(new Task.Modal(event.getProject(), "Pasting Bookmark", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    bookmarksService.pasteAfter(parentFolderId, bookmarkId, indicator);
                } catch (BookmarksException e) {
                    ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showMessageDialog(e.getMessage(), "Could not paste", Messages.getErrorIcon()));
                }
            }
        });
    }
}