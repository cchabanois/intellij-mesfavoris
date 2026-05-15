package mesfavoris.internal.markers.highlighters;

import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAware;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.icons.MesFavorisIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
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
     * Creates a stacked icon: a slightly offset semi-transparent copy peeks out from
     * behind the main icon, giving a clear "multiple pages" look.
     */
    private Icon createStackedIcon(Icon baseIcon) {
        int iconWidth = baseIcon.getIconWidth();
        int iconHeight = baseIcon.getIconHeight();
        int offset = 3;
        int totalWidth = iconWidth + offset;
        int totalHeight = iconHeight + offset;

        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                try {
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Back copy shifted down-right — clearly visible behind the front icon
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
                    baseIcon.paintIcon(c, g2d, x + offset, y + offset);

                    // Front icon fully opaque
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                    baseIcon.paintIcon(c, g2d, x, y);
                } finally {
                    g2d.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return totalWidth;
            }

            @Override
            public int getIconHeight() {
                return totalHeight;
            }
        };
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

