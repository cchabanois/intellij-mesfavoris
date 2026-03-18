package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import mesfavoris.model.Bookmark;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AddShortcutBookmarkAction extends AbstractAddBookmarkAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // Delegate to the abstract superclass to run the bookmark addition task
        // The DataContext will contain the selected bookmark, which the IBookmarksService
        // will use to determine the properties for the new shortcut bookmark.
        runAddBookmarkTask(event, event.getDataContext());
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable the action only if a single bookmark is selected
        List<Bookmark> bookmarks = getSelectedBookmarks(e);
        e.getPresentation().setEnabledAndVisible(bookmarks.size() == 1);
    }
}
