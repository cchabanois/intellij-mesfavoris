package mesfavoris.internal.markers;

import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAware;
import com.intellij.util.ui.ImageUtil;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.icons.MesFavorisIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

/**
 * Gutter icon renderer for multiple bookmarks on the same line.
 * Displays the bookmark icon with a badge indicating multiple bookmarks.
 */
public class GroupedBookmarkGutterIconRenderer extends GutterIconRenderer implements DumbAware {
    private final List<BookmarkMarker> bookmarkMarkers;
    private Icon cachedIcon;

    public GroupedBookmarkGutterIconRenderer(List<BookmarkMarker> bookmarkMarkers) {
        if (bookmarkMarkers == null || bookmarkMarkers.isEmpty()) {
            throw new IllegalArgumentException("bookmarkMarkers must not be null or empty");
        }
        this.bookmarkMarkers = List.copyOf(bookmarkMarkers);
    }

    @Override
    public @NotNull Icon getIcon() {
        if (cachedIcon == null) {
            cachedIcon = createIconWithBadge();
        }
        return cachedIcon;
    }

    public List<BookmarkMarker> getBookmarkMarkers() {
        return bookmarkMarkers;
    }

    /**
     * Creates an icon with a stacked effect indicating multiple bookmarks
     */
    private Icon createIconWithBadge() {
        Icon baseIcon = MesFavorisIcons.bookmark;
        int count = bookmarkMarkers.size();

        if (count <= 1) {
            return baseIcon;
        }

        // Create stacked icon effect
        return createStackedIcon(baseIcon);
    }

    /**
     * Creates a stacked icon by drawing the base icon multiple times with slight offsets
     */
    private Icon createStackedIcon(Icon baseIcon) {
        int iconWidth = baseIcon.getIconWidth();
        int iconHeight = baseIcon.getIconHeight();
        int offset = 2; // Offset for stacking effect

        // Create image large enough for stacked icons
        int totalWidth = iconWidth + offset * 2;
        int totalHeight = iconHeight + offset * 2;

        BufferedImage image = ImageUtil.createImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        try {
            // Enable antialiasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw base icon three times with offsets to create stacked effect
            // Back layer (most transparent)
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            baseIcon.paintIcon(null, g2d, offset * 2, offset * 2);

            // Middle layer
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            baseIcon.paintIcon(null, g2d, offset, offset);

            // Front layer (fully opaque)
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            baseIcon.paintIcon(null, g2d, 0, 0);
        } finally {
            g2d.dispose();
        }

        return new ImageIcon(image);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupedBookmarkGutterIconRenderer that = (GroupedBookmarkGutterIconRenderer) o;
        return bookmarkMarkers.equals(that.bookmarkMarkers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookmarkMarkers);
    }

    @Override
    public String toString() {
        return "GroupedBookmarkGutterIconRenderer{" +
                "count=" + bookmarkMarkers.size() +
                '}';
    }
}

