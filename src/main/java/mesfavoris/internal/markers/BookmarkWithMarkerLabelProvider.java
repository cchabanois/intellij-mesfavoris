package mesfavoris.internal.markers;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.ui.ImageUtil;
import mesfavoris.IBookmarksMarkers;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.service.BookmarksService;
import mesfavoris.ui.renderers.StyledString;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BookmarkWithMarkerLabelProvider implements IBookmarkLabelProvider {
    private final IBookmarksMarkers bookmarksMarkers;
    private final IBookmarkLabelProvider parentBookmarkLabelProvider;
    private final Icon markerIndicator;

    public BookmarkWithMarkerLabelProvider(Project project, IBookmarkLabelProvider parentBookmarkLabelProvider) {
        this.parentBookmarkLabelProvider = parentBookmarkLabelProvider;
        BookmarksService bookmarksService = project.getService(BookmarksService.class);
        this.bookmarksMarkers = bookmarksService.getBookmarksMarkers();
        this.markerIndicator = createMarkerIndicator();
    }

    @Override
    public StyledString getStyledText(Context context, Bookmark bookmark) {
        return parentBookmarkLabelProvider.getStyledText(context, bookmark);
    }

    @Override
    public Icon getIcon(Context context, Bookmark bookmark) {
        Icon baseIcon = parentBookmarkLabelProvider.getIcon(context, bookmark);
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
     * Creates a small orange dot icon to indicate the presence of a marker
     */
    private Icon createMarkerIndicator() {
        int size = 6; // Small dot size
        BufferedImage image = ImageUtil.createImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Enable antialiasing for smooth circle
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw orange circle
        g2d.setColor(new JBColor(new Color(255, 165, 0), new Color(255, 165, 0))); // Orange color
        g2d.fillOval(0, 0, size - 1, size - 1);

        // Add a subtle border
        g2d.setColor(new JBColor(new Color(200, 120, 0), new Color(200, 120, 0))); // Darker orange for border
        g2d.drawOval(0, 0, size - 1, size - 1);

        g2d.dispose();

        return new ImageIcon(image);
    }

    @Override
    public boolean canHandle(Context context, Bookmark bookmark) {
        return parentBookmarkLabelProvider.canHandle(context, bookmark);
    }
}
