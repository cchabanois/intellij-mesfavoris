package mesfavoris.internal.markers;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.ui.JBUI;
import mesfavoris.IBookmarksMarkers;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.service.IBookmarksService;
import mesfavoris.ui.renderers.StyledString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class BookmarkWithMarkerLabelProvider implements IBookmarkLabelProvider {
    private final IBookmarksMarkers bookmarksMarkers;
    private final IBookmarkLabelProvider parentBookmarkLabelProvider;
    private final Icon markerIndicator;

    public BookmarkWithMarkerLabelProvider(Project project, IBookmarkLabelProvider parentBookmarkLabelProvider) {
        this.parentBookmarkLabelProvider = parentBookmarkLabelProvider;
        IBookmarksService bookmarksService = project.getService(IBookmarksService.class);
        this.bookmarksMarkers = bookmarksService.getBookmarksMarkers();
        this.markerIndicator = createMarkerIndicator();
    }

    @Override
    public StyledString getStyledText(@Nullable Project project, @NotNull Bookmark bookmark) {
        return parentBookmarkLabelProvider.getStyledText(project, bookmark);
    }

    @Override
    public Icon getIcon(@Nullable Project project, @NotNull Bookmark bookmark) {
        Icon baseIcon = parentBookmarkLabelProvider.getIcon(project, bookmark);
        if (baseIcon == null) {
            return null;
        }
        BookmarkMarker marker = bookmarksMarkers.getMarker(bookmark.getId());
        if (marker == null) {
            return baseIcon;
        }

        // Create a layered icon with the base icon and the marker indicator
        LayeredIcon layeredIcon = new LayeredIcon(2);
        layeredIcon.setIcon(baseIcon, 0);
        layeredIcon.setIcon(markerIndicator, 1, SwingConstants.SOUTH_EAST);

        return layeredIcon;
    }

    /**
     * Creates a small orange dot icon that scales correctly on all displays (including HiDPI).
     */
    private Icon createMarkerIndicator() {
        return new Icon() {
            private final int logicalSize = 6;

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                try {
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // The actual size to draw is scaled from the logical size
                    int actualSize = getIconWidth();

                    g2d.setColor(new JBColor(new Color(255, 165, 0), new Color(255, 165, 0))); // Orange color
                    g2d.fillOval(x, y, actualSize - 1, actualSize - 1);

                    g2d.setColor(new JBColor(new Color(200, 120, 0), new Color(200, 120, 0))); // Darker orange for border
                    g2d.drawOval(x, y, actualSize - 1, actualSize - 1);
                } finally {
                    g2d.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                // The icon's logical size is 6, scaled by the UI toolkit for the current display
                return JBUI.scale(logicalSize);
            }

            @Override
            public int getIconHeight() {
                // The icon's logical size is 6, scaled by the UI toolkit for the current display
                return JBUI.scale(logicalSize);
            }
        };
    }

    @Override
    public boolean canHandle(@Nullable Project project, @NotNull Bookmark bookmark) {
        return parentBookmarkLabelProvider.canHandle(project, bookmark);
    }
}