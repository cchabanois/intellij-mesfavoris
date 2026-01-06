package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import mesfavoris.IBookmarksMarkers;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.service.IBookmarksService;
import mesfavoris.ui.renderers.BookmarksListCellRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Action to delete bookmark markers for selected bookmarks.
 * Only bookmarks that have associated markers will be processed.
 *
 * @author cchabanois
 */
public class DeleteBookmarkMarkerAction extends AbstractBookmarkAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        List<Bookmark> bookmarks = getSelectedBookmarks(event);
        boolean hasBookmarksWithMarkers = hasBookmarksWithMarkers(event, bookmarks);

        // Only enable the action if focus is on the bookmarks tree and there are bookmarks with markers
        boolean focusOnTree = isFocusOnBookmarksTree();

        event.getPresentation().setEnabledAndVisible(hasBookmarksWithMarkers && focusOnTree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Set<Bookmark> bookmarksWithMarkers = getSelectedBookmarksRecursively(event, bookmark -> hasMarker(event, bookmark));
        if (bookmarksWithMarkers.isEmpty()) {
            return;
        }

        Project project = event.getProject();
        List<Bookmark> bookmarksList = new ArrayList<>(bookmarksWithMarkers);

        if (showConfirmationDialog(project, bookmarksList)) {
            deleteMarkers(event, bookmarksList);
        }
    }

    private boolean hasBookmarksWithMarkers(AnActionEvent event, List<Bookmark> bookmarks) {
        if (bookmarks.isEmpty()) {
            return false;
        }

        // Check if any of the selected bookmarks (recursively) have markers
        Set<Bookmark> allBookmarks = getSelectedBookmarksRecursively(event, bookmark -> hasMarker(event, bookmark));
        return !allBookmarks.isEmpty();
    }

    private boolean hasMarker(AnActionEvent event, Bookmark bookmark) {
        if (bookmark == null) {
            return false;
        }
        IBookmarksService bookmarksService = getBookmarksService(event);
        IBookmarksMarkers bookmarksMarkers = bookmarksService.getBookmarksMarkers();
        BookmarkMarker marker = bookmarksMarkers.getMarker(bookmark.getId());
        return marker != null;
    }

    private void deleteMarkers(AnActionEvent event, List<Bookmark> bookmarks) {
        IBookmarksService bookmarksService = getBookmarksService(event);
        IBookmarksMarkers bookmarksMarkers = bookmarksService.getBookmarksMarkers();

        int deletedCount = 0;
        for (Bookmark bookmark : bookmarks) {
            if (hasMarker(event, bookmark)) {
                bookmarksMarkers.deleteMarker(bookmark.getId());
                deletedCount++;
            }
        }

        if (deletedCount > 0) {
            String message = String.format("Deleted %d bookmark marker%s", deletedCount, deletedCount > 1 ? "s" : "");
            Messages.showInfoMessage(event.getProject(), message, "Delete Bookmark Markers");
        }
    }

    private boolean showConfirmationDialog(Project project, List<Bookmark> bookmarksWithMarkers) {
        JBList<Bookmark> list = new JBList<>(JBList.createDefaultListModel(bookmarksWithMarkers));
        IBookmarksService bookmarksService = project.getService(IBookmarksService.class);
        BookmarkDatabase bookmarkDatabase = bookmarksService.getBookmarkDatabase();
        list.setCellRenderer(new BookmarksListCellRenderer(project, bookmarkDatabase, 
                bookmarksService.getBookmarksDirtyStateTracker(),
                bookmarksService.getBookmarkLabelProvider()));

        String labelText = String.format("Are you sure you want to delete markers for these %d bookmark%s?", 
                bookmarksWithMarkers.size(), bookmarksWithMarkers.size() > 1 ? "s" : "");
        JBLabel label = new JBLabel(labelText);
        label.setBorder(JBUI.Borders.emptyBottom(10));
        list.setBorder(JBUI.Borders.emptyTop(6));
        JBScrollPane scrollPane = new JBScrollPane(list);

        JPanel content = new JPanel(new BorderLayout());
        content.add(label, BorderLayout.NORTH);
        content.add(scrollPane, BorderLayout.CENTER);
        content.setBorder(JBUI.Borders.empty(10));

        DialogBuilder dialogBuilder = new DialogBuilder(project)
                .title("Delete Bookmark Markers")
                .centerPanel(content);

        dialogBuilder.addOkAction();
        dialogBuilder.addCancelAction();
        return dialogBuilder.showAndGet();
    }
}
