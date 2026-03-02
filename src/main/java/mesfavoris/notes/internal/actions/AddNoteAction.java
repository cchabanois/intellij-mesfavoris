package mesfavoris.notes.internal.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import mesfavoris.BookmarksException;
import mesfavoris.internal.actions.AbstractBookmarkAction;
import mesfavoris.model.Bookmark;
import mesfavoris.notes.NoteBookmarkProperties;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class AddNoteAction extends AbstractBookmarkAction {
    private static final Logger LOG = Logger.getInstance(AddNoteAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        IBookmarksService bookmarksService = getBookmarksService(e);
        if (bookmarksService == null) {
            return;
        }

        try {
            bookmarksService.addBookmark(Map.of(
                    Bookmark.PROPERTY_NAME, "New Note",
                    NoteBookmarkProperties.PROP_NOTES, ""
            ));
        } catch (BookmarksException ex) {
            LOG.error("Could not add note bookmark", ex);
            Messages.showErrorDialog(project, "Cannot add note: " + ex.getMessage(), "Add Note Failed");
        }
    }
}
