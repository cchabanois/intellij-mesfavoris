package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import mesfavoris.internal.model.merge.BookmarksTreeIterable;
import mesfavoris.internal.model.merge.BookmarksTreeIterator;
import mesfavoris.internal.toolwindow.BookmarksTreeComponent;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.service.IBookmarksService;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class AbstractBookmarkAction extends AnAction implements DumbAware {

    protected IBookmarksService getBookmarksService(AnActionEvent event) {
        Project project = event.getProject();
        return project.getService(IBookmarksService.class);
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

    protected Set<Bookmark> getSelectedBookmarksRecursively(AnActionEvent event, Predicate<Bookmark> filter) {
        BookmarksTree bookmarksTree = getBookmarksService(event).getBookmarksTree();
        Set<Bookmark> bookmarkIds = new LinkedHashSet<>();
        for (Bookmark bookmark : getSelectedBookmarks(event)) {
            if (bookmark instanceof BookmarkFolder) {
                bookmarkIds.addAll(getBookmarksRecursively(bookmarksTree, bookmark.getId(), filter));
            } else if (bookmark != null && filter.test(bookmark)) {
                bookmarkIds.add(bookmark);
            }
        }
        return bookmarkIds;
    }

    private List<Bookmark> getBookmarksRecursively(BookmarksTree bookmarksTree, BookmarkId folderId, Predicate<Bookmark> filter) {
        BookmarksTreeIterable bookmarksTreeIterable = new BookmarksTreeIterable(bookmarksTree, folderId,
                BookmarksTreeIterator.Algorithm.PRE_ORDER);
        return StreamSupport.stream(bookmarksTreeIterable.spliterator(), false).filter(filter).collect(Collectors.toList());
    }

}
