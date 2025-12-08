package mesfavoris.internal.actions;

import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil;
import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.util.Consumer;
import mesfavoris.BookmarksException;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class NewBookmarkFolderAction extends AbstractBookmarkAction {
    private static final Logger LOG = Logger.getInstance(NewBookmarkFolderAction.class);

    @Override
    public void update(@NotNull AnActionEvent event) {
        BookmarkId parentId = getParentId(event);
        event.getPresentation().setEnabledAndVisible(parentId != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        IBookmarksService bookmarksService = getBookmarksService(event);
        BookmarkId parentId = getParentId(event);
        if (parentId == null) {
            return;
        }
        askBookmarkFolderName(event.getProject(), (folderName) -> {
            try {
                bookmarksService.addBookmarkFolder(parentId, folderName);
            } catch (BookmarksException e) {
                LOG.error("Could not add bookmark folder", e);
                Messages.showErrorDialog(
                        event.getProject(),
                        "Cannot add bookmark folder : " + e.getMessage(),
                        "Cannot Add Bookmark Folder"
                );
            }

        });
    }

    private BookmarkId  getParentId(@NotNull AnActionEvent event) {
        IBookmarksService bookmarksService = getBookmarksService(event);
        Bookmark bookmark = getSelectedBookmark(event);
        BookmarkFolder parent;
        if (bookmark == null) {
            parent = bookmarksService.getBookmarksTree().getRootFolder();
        } else {
            if (!(bookmark instanceof BookmarkFolder)) {
                return null;
            }
            parent = (BookmarkFolder) bookmark;
        }
        return parent.getId();
    }

    private void askBookmarkFolderName(Project project, Consumer<String> applyAction) {
        NewItemSimplePopupPanel contentPanel = new NewItemSimplePopupPanel();
        JTextField nameField = contentPanel.getTextField();
        JBPopup popup = NewItemPopupUtil.createNewItemPopup("New Folder", contentPanel, nameField);
        contentPanel.setApplyAction(inputEvent -> {
            if (nameField.getText().trim().isEmpty()) {
                return;
            }
            popup.closeOk(inputEvent);
            applyAction.consume(nameField.getText());
        });
        popup.showCenteredInCurrentWindow(project);
    }


}
