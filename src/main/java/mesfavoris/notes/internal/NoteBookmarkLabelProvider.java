package mesfavoris.notes.internal;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.AbstractBookmarkLabelProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.notes.NoteBookmarkProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class NoteBookmarkLabelProvider extends AbstractBookmarkLabelProvider {

    @Override
    public Icon getIcon(@Nullable Project project, @NotNull Bookmark bookmark) {
        return AllIcons.General.Note;
    }

    @Override
    public boolean canHandle(@Nullable Project project, @NotNull Bookmark bookmark) {
        return bookmark.getPropertyValue(NoteBookmarkProperties.PROP_NOTES) != null;
    }
}
