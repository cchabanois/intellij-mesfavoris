package mesfavoris.ui.renderers;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import mesfavoris.bookmarktype.BookmarkDatabaseLabelProviderContext;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.commons.Adapters;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class BookmarksTreeCellRenderer extends ColoredTreeCellRenderer {
    private final BookmarkDatabase bookmarkDatabase;
    private final IBookmarkLabelProvider bookmarkLabelProvider;
    private final BookmarkDatabaseLabelProviderContext context;
    private final Color commentColor = new Color(63, 127, 95);

    public BookmarksTreeCellRenderer(Project project, BookmarkDatabase bookmarkDatabase, IBookmarkLabelProvider bookmarkLabelProvider) {
        this.bookmarkDatabase = bookmarkDatabase;
        this.bookmarkLabelProvider = bookmarkLabelProvider;
        this.context = new BookmarkDatabaseLabelProviderContext(project, bookmarkDatabase);
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        StyledString styledText = getStyledText(value);
        styledText.appendTo(this);
        Icon icon = getIcon(value);
        setIcon(icon);
    }

    private Icon getIcon(final Object element) {
        Bookmark bookmark = (Bookmark) Adapters.adapt(element, Bookmark.class);
        return bookmarkLabelProvider.getIcon(context, bookmark);
    }

    private StyledString getStyledText(final Object element) {
        Bookmark bookmark = (Bookmark) Adapters.adapt(element, Bookmark.class);
        String comment = getFirstCommentLine(bookmark);
        boolean hasComment = comment != null && comment.trim().length() > 0;
        boolean isDisabled = false; //isUnderDisconnectedRemoteBookmarkFolder(bookmark);
        StyledString styledString = new StyledString();
//        if (dirtyBookmarksProvider.getDirtyBookmarks().contains(bookmark.getId())) {
//            styledString = styledString.append("> ");
//        }
        styledString = styledString.append(bookmarkLabelProvider.getStyledText(context, bookmark));
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

}
