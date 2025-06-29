package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import mesfavoris.internal.toolwindow.BookmarksTreeComponent;
import mesfavoris.model.Bookmark;
import mesfavoris.service.BookmarksService;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractBookmarkAction extends AnAction  {

    protected BookmarksService getBookmarksService(AnActionEvent event) {
        Project project = event.getProject();
        return project.getService(BookmarksService.class);
    }


    protected List<Bookmark> getSelectedBookmarks(AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        Object[] objects = dataContext.getData(PlatformDataKeys.SELECTED_ITEMS);
        if (objects == null) {
            return Collections.emptyList();
        } else {
            return Arrays.stream(objects).filter(object -> object instanceof Bookmark).map(object -> (Bookmark) object).toList();
        }
    }

    protected Bookmark getSelectedBookmark(AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        Object object = dataContext.getData(PlatformDataKeys.SELECTED_ITEM);
        return object instanceof Bookmark bookmark ? bookmark : null;
    }

    /**
     * Check if the current focus is on a BookmarksTreeComponent
     */
    protected boolean isFocusOnBookmarksTree() {
        // Walk up the component hierarchy to find if we're inside a BookmarksTreeComponent
        Component current = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        while (current != null) {
            if (current instanceof BookmarksTreeComponent) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
