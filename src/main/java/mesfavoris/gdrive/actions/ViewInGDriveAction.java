package mesfavoris.gdrive.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import mesfavoris.gdrive.mappings.BookmarkMapping;
import mesfavoris.gdrive.mappings.BookmarkMappingsStore;
import mesfavoris.gdrive.operations.ViewInGDriveOperation;
import mesfavoris.internal.actions.AbstractBookmarkAction;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class ViewInGDriveAction extends AbstractBookmarkAction {

    public ViewInGDriveAction() {
        super();
        getTemplatePresentation().setText("View in Google Drive");
        getTemplatePresentation().setDescription("Open the bookmark folder in Google Drive");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        BookmarkFolder bookmarkFolder = getSelectedBookmarkFolder(event);
        if (bookmarkFolder == null) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }

        Project project = event.getProject();
        if (project == null) {
            event.getPresentation().setEnabledAndVisible(false);
            return;
        }

        BookmarkMappingsStore bookmarkMappingsStore = project.getService(BookmarkMappingsStore.class);
        Optional<String> fileId = bookmarkMappingsStore.getMapping(bookmarkFolder.getId())
                .map(BookmarkMapping::getFileId);

        event.getPresentation().setEnabledAndVisible(fileId.isPresent());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        BookmarkFolder bookmarkFolder = getSelectedBookmarkFolder(event);
        if (bookmarkFolder == null) {
            return;
        }

        Project project = event.getProject();
        if (project == null) {
            return;
        }

        BookmarkMappingsStore bookmarkMappingsStore = project.getService(BookmarkMappingsStore.class);
        Optional<String> fileId = bookmarkMappingsStore.getMapping(bookmarkFolder.getId())
                .map(BookmarkMapping::getFileId);

        if (fileId.isEmpty()) {
            return;
        }

        ViewInGDriveOperation operation = new ViewInGDriveOperation();
        operation.viewInGDrive(fileId.get());
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
}

