package mesfavoris.internal.search;

import com.intellij.icons.AllIcons;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarksTree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Renderer for bookmarks in Search Everywhere dialog with right-aligned location
 */
public class BookmarksSearchEverywhereRenderer implements ListCellRenderer<BookmarkItem> {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final SimpleColoredComponent mainComponent = new SimpleColoredComponent();
    private final SimpleColoredComponent locationComponent = new SimpleColoredComponent();

    public BookmarksSearchEverywhereRenderer() {
        panel.add(mainComponent, BorderLayout.CENTER);
        panel.add(locationComponent, BorderLayout.EAST);
        panel.setBorder(JBUI.Borders.empty(1, 2));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends BookmarkItem> list,
                                                   BookmarkItem value,
                                                   int index,
                                                   boolean selected,
                                                   boolean hasFocus) {
        mainComponent.clear();
        locationComponent.clear();

        if (value == null) {
            return panel;
        }

        // Set colors
        Color bg = selected ? list.getSelectionBackground() : list.getBackground();

        panel.setBackground(bg);
        mainComponent.setBackground(bg);
        locationComponent.setBackground(bg);

        Bookmark bookmark = value.getBookmark();

        // Set icon
        if (bookmark instanceof BookmarkFolder) {
            mainComponent.setIcon(AllIcons.Nodes.Folder);
        } else {
            mainComponent.setIcon(AllIcons.Nodes.Bookmark);
        }

        // Set name
        String name = bookmark.getPropertyValue(Bookmark.PROPERTY_NAME);
        if (name == null || name.isEmpty()) {
            name = "unnamed";
        }
        mainComponent.append(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);

        // Set comment as grayed text
        String comment = bookmark.getPropertyValue(Bookmark.PROPERTY_COMMENT);
        if (comment != null && !comment.isEmpty()) {
            mainComponent.append("  " + comment, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        // Show folder path on the right
        String path = getBookmarkPath(value);
        if (path != null && !path.isEmpty()) {
            locationComponent.append(path, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
        }

        return panel;
    }

    /**
     * Build the folder path for a bookmark (e.g., "Folder1 > Folder2 > Folder3")
     */
    private String getBookmarkPath(@NotNull BookmarkItem item) {
        BookmarksTree bookmarksTree = item.getBookmarksTree();
        if (bookmarksTree == null) {
            return null;
        }

        List<String> pathParts = new ArrayList<>();

        BookmarkFolder parent = bookmarksTree.getParentBookmark(item.getBookmark().getId());
        while (parent != null && !parent.getId().equals(bookmarksTree.getRootFolder().getId())) {
            String parentName = parent.getPropertyValue(Bookmark.PROPERTY_NAME);
            if (parentName != null && !parentName.isEmpty()) {
                pathParts.add(parentName);
            }
            parent = bookmarksTree.getParentBookmark(parent.getId());
        }

        if (pathParts.isEmpty()) {
            return null;
        }

        Collections.reverse(pathParts);
        return String.join(" > ", pathParts);
    }
}

