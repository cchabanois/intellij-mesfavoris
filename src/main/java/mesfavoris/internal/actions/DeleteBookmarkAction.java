package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import mesfavoris.BookmarksException;
import mesfavoris.internal.bookmarktypes.BookmarkLabelProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarksTree;
import mesfavoris.service.BookmarksService;
import mesfavoris.texteditor.internal.TextEditorBookmarkLabelProvider;
import mesfavoris.ui.renderers.BookmarkFolderLabelProvider;
import mesfavoris.ui.renderers.BookmarksListCellRenderer;
import mesfavoris.url.internal.UrlBookmarkLabelProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class DeleteBookmarkAction extends AbstractBookmarkAction  {

    @Override
    public void update(@NotNull AnActionEvent event) {
        List<Bookmark> bookmarks = getSelectedBookmarks(event);
        event.getPresentation().setEnabledAndVisible(!bookmarks.isEmpty());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        List<Bookmark> bookmarks = getSelectedBookmarks(event);
        if (bookmarks.isEmpty()) {
            return;
        }
        Project project = event.getProject();
        BookmarksService bookmarksService = getBookmarksService(event);
        BookmarkDatabase bookmarkDatabase = bookmarksService.getBookmarkDatabase();
        List<Bookmark> bookmarksToDelete = new ArrayList<>(getBookmarksToDelete(bookmarkDatabase.getBookmarksTree(), bookmarks));
        if (showConfirmationDialog(project, bookmarksToDelete)) {
            try {
                bookmarksService.deleteBookmarks(bookmarksToDelete.stream().map(Bookmark::getId).toList(), true);
            } catch (BookmarksException e) {
                Messages.showMessageDialog(e.getMessage(), "Could Not Delete Bookmarks", Messages.getInformationIcon());
            }
        }
    }

    private Set<Bookmark> getBookmarksToDelete(BookmarksTree bookmarksTree, List<Bookmark> selection) {
        Set<Bookmark> bookmarks = new LinkedHashSet<>();
        for (Bookmark bookmark : selection) {
            bookmarks.add(bookmark);
/*            if (bookmark instanceof BookmarkFolder
                    && remoteBookmarksStoreManager.getRemoteBookmarkFolder(bookmark.getId()).isPresent()) {
                bookmarks.addAll(getBookmarksRecursively(bookmarksTree, bookmark.getId(), b->true));
            } */
        }
        return bookmarks;
    }

    private boolean showConfirmationDialog(Project project, List<Bookmark> bookmarksToDelete) {
        JBList<Bookmark> list = new JBList<>(JBList.createDefaultListModel(bookmarksToDelete));
        BookmarksService bookmarksService = project.getService(BookmarksService.class);
        BookmarkDatabase bookmarkDatabase = bookmarksService.getBookmarkDatabase();
        list.setCellRenderer(new BookmarksListCellRenderer(project, bookmarkDatabase, bookmarksService.getBookmarksDirtyStateTracker(),
                new BookmarkLabelProvider(Arrays.asList(new UrlBookmarkLabelProvider(), new BookmarkFolderLabelProvider(), new TextEditorBookmarkLabelProvider()))));

        JBLabel label = new JBLabel("Are you sure you want to delete these bookmarks ?");
        label.setBorder(JBUI.Borders.emptyBottom(10));
        list.setBorder(JBUI.Borders.emptyTop(6));
        JBScrollPane scrollPane = new JBScrollPane(list);

        JPanel content = new JPanel(new BorderLayout());
        content.add(label, BorderLayout.NORTH);
        content.add(scrollPane, BorderLayout.CENTER);
        content.setBorder(JBUI.Borders.empty(10));

        DialogBuilder dialogBuilder = new DialogBuilder(project)
                .title("Delete Bookmarks")
                .centerPanel(content);

        dialogBuilder.addOkAction();
        dialogBuilder.addCancelAction();
        return dialogBuilder.showAndGet();
    }

}
