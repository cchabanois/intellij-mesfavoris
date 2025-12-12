package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import mesfavoris.BookmarksException;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarkFolder;
import mesfavoris.service.IBookmarksService;
import mesfavoris.ui.renderers.BookmarksListCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * Action to delete a shared bookmark folder.
 * Shows a confirmation dialog with option to also delete from remote store.
 */
public class DeleteSharedBookmarkFolderAction extends AbstractBookmarkAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(DeleteSharedBookmarkFolderAction.class);

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }

        BookmarkFolder selectedFolder = getSelectedBookmarkFolder(event);
        if (selectedFolder == null) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }

        IBookmarksService bookmarksService = getBookmarksService(event);
        boolean isRemoteFolder = bookmarksService.getRemoteBookmarkFolder(selectedFolder.getId()).isPresent();
        event.getPresentation().setEnabledAndVisible(isRemoteFolder);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        BookmarkFolder selectedFolder = getSelectedBookmarkFolder(event);
        if (selectedFolder == null) {
            return;
        }

        IBookmarksService bookmarksService = getBookmarksService(event);
        RemoteBookmarkFolder remoteBookmarkFolder = bookmarksService
                .getRemoteBookmarkFolder(selectedFolder.getId())
                .orElse(null);

        if (remoteBookmarkFolder == null) {
            return;
        }

        ConfirmDeleteDialog dialog = new ConfirmDeleteDialog(project, selectedFolder, remoteBookmarkFolder, bookmarksService);
        if (!dialog.showAndGet()) {
            return;
        }

        boolean deleteFromRemoteStore = dialog.isDeleteFromRemoteStore();
        String storeId = remoteBookmarkFolder.getRemoteBookmarkStoreId();

        ProgressManager.getInstance().run(new Task.Modal(project, "Deleting Shared Bookmark Folder", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    if (deleteFromRemoteStore) {
                        indicator.setText("Removing from remote store...");
                        bookmarksService.removeFromRemoteBookmarksStore(storeId, selectedFolder.getId(), indicator);
                    }
                    indicator.setText("Deleting bookmark folder...");
                    bookmarksService.deleteBookmarks(Collections.singletonList(selectedFolder.getId()), true);
                    showSuccessMessage(project);
                } catch (BookmarksException e) {
                    LOG.error("Failed to delete shared bookmark folder", e);
                    showErrorMessage(project, e);
                }
            }
        });
    }

    private BookmarkFolder getSelectedBookmarkFolder(@NotNull AnActionEvent event) {
        List<Bookmark> selectedBookmarks = getSelectedBookmarks(event);
        if (selectedBookmarks.size() != 1) {
            return null;
        }

        Bookmark bookmark = selectedBookmarks.get(0);
        if (!(bookmark instanceof BookmarkFolder)) {
            return null;
        }

        return (BookmarkFolder) bookmark;
    }

    private void showSuccessMessage(@NotNull Project project) {
        ApplicationManager.getApplication().invokeLater(() ->
                Messages.showInfoMessage(
                        project,
                        "Shared bookmark folder successfully deleted",
                        "Folder Deleted"
                ));
    }

    private void showErrorMessage(@NotNull Project project, @NotNull BookmarksException e) {
        ApplicationManager.getApplication().invokeLater(() ->
                Messages.showErrorDialog(
                        project,
                        "Could not delete shared bookmark folder: " + e.getMessage(),
                        "Error Deleting Folder"
                ));
    }

    /**
     * Confirmation dialog for deleting a shared bookmark folder
     */
    private static class ConfirmDeleteDialog extends DialogWrapper {
        private final BookmarkFolder bookmarkFolder;
        private final RemoteBookmarkFolder remoteBookmarkFolder;
        private final IBookmarksService bookmarksService;
        private final Project project;
        private JBCheckBox deleteFromRemoteStoreCheckBox;

        public ConfirmDeleteDialog(@NotNull Project project,
                                   @NotNull BookmarkFolder bookmarkFolder,
                                   @NotNull RemoteBookmarkFolder remoteBookmarkFolder,
                                   @NotNull IBookmarksService bookmarksService) {
            super(project);
            this.project = project;
            this.bookmarkFolder = bookmarkFolder;
            this.remoteBookmarkFolder = remoteBookmarkFolder;
            this.bookmarksService = bookmarksService;

            setTitle("Remove Shared Bookmark Folder");
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout(JBUI.scale(10), JBUI.scale(10)));
            panel.setBorder(JBUI.Borders.empty(10));

            // Message label
            JBLabel messageLabel = new JBLabel("Are you sure you want to remove this shared bookmark folder?");
            messageLabel.setBorder(JBUI.Borders.emptyBottom(10));
            panel.add(messageLabel, BorderLayout.NORTH);

            // Bookmark list
            JBList<Bookmark> list = new JBList<>(JBList.createDefaultListModel(Collections.singletonList(bookmarkFolder)));
            BookmarkDatabase bookmarkDatabase = bookmarksService.getBookmarkDatabase();
            list.setCellRenderer(new BookmarksListCellRenderer(project, bookmarkDatabase,
                    bookmarksService.getBookmarksDirtyStateTracker(),
                    bookmarksService.getBookmarkLabelProvider()));
            list.setBorder(JBUI.Borders.emptyTop(6));

            JBScrollPane scrollPane = new JBScrollPane(list);
            scrollPane.setPreferredSize(JBUI.size(400, 60));
            panel.add(scrollPane, BorderLayout.CENTER);

            // Checkbox for deleting from remote store
            IRemoteBookmarksStore remoteBookmarkStore = bookmarksService
                    .getRemoteBookmarksStore(remoteBookmarkFolder.getRemoteBookmarkStoreId())
                    .orElse(null);

            if (remoteBookmarkStore != null) {
                deleteFromRemoteStoreCheckBox = new JBCheckBox("Delete from " + remoteBookmarkStore.getDescriptor().label());
                boolean isReadOnly = Boolean.TRUE.toString()
                        .equalsIgnoreCase(remoteBookmarkFolder.getProperties().get(RemoteBookmarkFolder.PROP_READONLY));
                deleteFromRemoteStoreCheckBox.setEnabled(
                        remoteBookmarkStore.getState() == IRemoteBookmarksStore.State.connected && !isReadOnly);
                deleteFromRemoteStoreCheckBox.setBorder(JBUI.Borders.emptyTop(10));
                panel.add(deleteFromRemoteStoreCheckBox, BorderLayout.SOUTH);
            }

            return panel;
        }

        public boolean isDeleteFromRemoteStore() {
            return deleteFromRemoteStoreCheckBox != null && deleteFromRemoteStoreCheckBox.isSelected();
        }
    }
}

