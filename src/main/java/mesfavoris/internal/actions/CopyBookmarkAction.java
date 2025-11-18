package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import mesfavoris.internal.service.operations.CopyBookmarkOperation;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class CopyBookmarkAction extends AbstractBookmarkAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        List<Bookmark> bookmarks = getSelectedBookmarks(event);
        boolean hasBookmarks = !bookmarks.isEmpty();

        // Only enable the action if focus is on the bookmarks tree
        boolean focusOnTree = isFocusOnBookmarksTree();

        event.getPresentation().setEnabledAndVisible(hasBookmarks && focusOnTree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        List<Bookmark> bookmarks = getSelectedBookmarks(event);
        if (bookmarks.isEmpty()) {
            return;
        }

        BookmarksService bookmarksService = getBookmarksService(event);
        BookmarksTree bookmarksTree = bookmarksService.getBookmarksTree();
        
        List<BookmarkId> bookmarkIds = bookmarks.stream()
                .map(Bookmark::getId)
                .collect(Collectors.toList());

        CopyBookmarkOperation operation = new CopyBookmarkOperation();
        operation.copyToClipboard(bookmarksTree, bookmarkIds);
    }
}

