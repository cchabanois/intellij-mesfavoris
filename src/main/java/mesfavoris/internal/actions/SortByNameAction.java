package mesfavoris.internal.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.Messages;
import mesfavoris.BookmarksException;
import mesfavoris.internal.service.operations.SortByNameOperation;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.service.BookmarksService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Action to sort bookmarks by name within a folder.
 * If a folder is selected, sorts its children.
 * If no selection or a non-folder is selected, sorts the root folder.
 */
public class SortByNameAction extends AbstractBookmarkAction implements DumbAware {

    @Override
    public void update(@NotNull AnActionEvent event) {
        // Only enable the action if focus is on the bookmarks tree
        boolean focusOnTree = isFocusOnBookmarksTree();
        event.getPresentation().setEnabledAndVisible(focusOnTree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        BookmarksService bookmarksService = getBookmarksService(event);
        BookmarkDatabase bookmarkDatabase = bookmarksService.getBookmarkDatabase();
        
        BookmarkFolder folderToSort = getFolderToSort(event, bookmarkDatabase);
        
        try {
            SortByNameOperation operation = new SortByNameOperation(bookmarkDatabase);
            operation.sortByName(folderToSort.getId());
        } catch (BookmarksException e) {
            ApplicationManager.getApplication().invokeLater(() ->
                Messages.showMessageDialog(e.getMessage(), "Could Not Sort Bookmarks", Messages.getErrorIcon()));
        }
    }

    private BookmarkFolder getFolderToSort(@NotNull AnActionEvent event, @NotNull BookmarkDatabase bookmarkDatabase) {
        List<Bookmark> selectedBookmarks = getSelectedBookmarks(event);
        
        if (selectedBookmarks.isEmpty()) {
            // No selection: sort root folder
            return bookmarkDatabase.getBookmarksTree().getRootFolder();
        }
        
        Bookmark firstSelected = selectedBookmarks.get(0);
        if (firstSelected instanceof BookmarkFolder) {
            // Folder selected: sort this folder
            return (BookmarkFolder) firstSelected;
        } else {
            // Non-folder selected: sort root folder
            return bookmarkDatabase.getBookmarksTree().getRootFolder();
        }
    }
}

