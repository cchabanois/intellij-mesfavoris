package mesfavoris.ui.renderers;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.commons.Adapters;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.persistence.IBookmarksDirtyStateTracker;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class BookmarksListCellRenderer extends ColoredListCellRenderer<Bookmark> {
    private final Project project;
    private final BookmarkDatabase bookmarkDatabase;
    private final IBookmarkLabelProvider bookmarkLabelProvider;
    private final IBookmarksDirtyStateTracker bookmarksDirtyStateTracker;
    private final Color commentColor = new JBColor(new Color(63, 127, 95), new Color(63, 127, 95));

    public BookmarksListCellRenderer(Project project, BookmarkDatabase bookmarkDatabase, IBookmarksDirtyStateTracker bookmarksDirtyStateTracker, IBookmarkLabelProvider bookmarkLabelProvider) {
        this.project = project;
        this.bookmarkDatabase = bookmarkDatabase;
        this.bookmarkLabelProvider = bookmarkLabelProvider;
        this.bookmarksDirtyStateTracker = bookmarksDirtyStateTracker;
    }

    private Icon getIcon(final Object element) {
        Bookmark bookmark = Adapters.adapt(element, Bookmark.class);
        return bookmarkLabelProvider.getIcon(project, bookmark);
    }

    private StyledString getStyledText(final Bookmark bookmark) {
        String comment = getFirstCommentLine(bookmark);
        boolean hasComment = comment != null && !comment.trim().isEmpty();
        boolean isDisabled = false; //isUnderDisconnectedRemoteBookmarkFolder(bookmark);
        StyledString styledString = new StyledString();
        if (bookmarksDirtyStateTracker.getDirtyBookmarks().contains(bookmark.getId())) {
            styledString = styledString.append("> ");
        }
        styledString = styledString.append(bookmarkLabelProvider.getStyledText(project, bookmark));
        if (isDisabled) {
            styledString = styledString.setStyle(SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        if (hasComment) {
            Color color = commentColor;
            Font font = null;
            if (isDisabled) {
                color = UIUtil.getInactiveTextColor();
            }
            styledString = styledString.append(" - " + comment, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color));
        }
        return styledString;
    }

    private String getFirstCommentLine(Bookmark bookmark) {
        String comment = bookmark.getPropertyValue(Bookmark.PROPERTY_COMMENT);
        if (comment == null) {
            return null;
        }
        try (Scanner scanner = new Scanner(comment)) {
            return scanner.nextLine();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @Override
    protected void customizeCellRenderer(@NotNull JList<? extends Bookmark> list, Bookmark value, int index, boolean selected, boolean hasFocus) {
        StyledString styledText = getStyledText(value);
        styledText.appendTo(this);
        Icon icon = getIcon(value);
        setIcon(icon);
    }
}
