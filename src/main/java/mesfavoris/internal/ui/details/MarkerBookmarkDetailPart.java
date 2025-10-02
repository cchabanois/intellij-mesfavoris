package mesfavoris.internal.ui.details;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import mesfavoris.IBookmarksMarkers;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.internal.markers.BookmarksMarkers.BookmarksMarkersListener;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.service.BookmarksService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Detail part that displays marker information for bookmarks that have an associated marker.
 * Shows the file name with icon and relative path, allows clicking to open the file at the marker's line.
 *
 * @author cchabanois
 */
public class MarkerBookmarkDetailPart extends AbstractBookmarkDetailPart {
    private MarkerInfoRenderer markerInfoRenderer;
    private BookmarkMarker currentMarker;
    private final IBookmarksMarkers bookmarksMarkers;

    public MarkerBookmarkDetailPart(Project project) {
        this(project, project.getService(BookmarksService.class).getBookmarkDatabase(), project.getService(BookmarksService.class).getBookmarksMarkers());
    }

    public MarkerBookmarkDetailPart(Project project, BookmarkDatabase bookmarkDatabase, IBookmarksMarkers bookmarksMarkers) {
        super(project, bookmarkDatabase);
        this.bookmarksMarkers = bookmarksMarkers;
        project.getMessageBus().connect(this).subscribe(BookmarksMarkersListener.TOPIC, new BookmarksMarkersListener() {
            @Override
            public void bookmarkMarkerDeleted(BookmarkMarker bookmarkMarker) {
                if (currentMarker != null && currentMarker.equals(bookmarkMarker)) {
                    ApplicationManager.getApplication().invokeLater(() -> updateMarkerInfo());                }
            }

            @Override
            public void bookmarkMarkerAdded(BookmarkMarker bookmarkMarker) {
                if (bookmark != null && bookmarkMarker.getBookmarkId().equals(bookmark.getId())) {
                    ApplicationManager.getApplication().invokeLater(() -> updateMarkerInfo());
                }
            }

            @Override
            public void bookmarkMarkerUpdated(BookmarkMarker previous, BookmarkMarker bookmarkMarker) {
                if (currentMarker != null && currentMarker.equals(previous)) {
                    ApplicationManager.getApplication().invokeLater(() -> updateMarkerInfo());
                }
            }
        });
    }

    @Override
    public String getTitle() {
        return "Marker";
    }

    @Override
    public JComponent createComponent() {
        JBPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));

        markerInfoRenderer = new MarkerInfoRenderer(project);
        markerInfoRenderer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        markerInfoRenderer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentMarker != null) {
                    openFileAtMarker();
                }
            }
        });

        panel.add(markerInfoRenderer, BorderLayout.NORTH);
        return panel;
    }

    @Override
    public void setBookmark(Bookmark bookmark) {
        super.setBookmark(bookmark);
        updateMarkerInfo();
    }

    private void updateMarkerInfo() {
        // Check if we have a marker for this bookmark
        BookmarkMarker marker = bookmarksMarkers.getMarker(bookmark.getId());

        // Update the renderer first (while currentMarker still has the old value)
        markerInfoRenderer.setMarker(marker);

        // Then update currentMarker
        currentMarker = marker;
    }

    private void openFileAtMarker() {
        if (currentMarker == null) {
            return;
        }

        VirtualFile file = currentMarker.getResource();
        int lineNumber = currentMarker.getLineNumber();

        // Use OpenFileDescriptor to open the file at the specific line
        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, lineNumber, 0);
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
    }

    @Override
    public boolean canHandle(Bookmark bookmark) {
        if (bookmark == null) {
            return false;
        }
        BookmarkMarker marker = bookmarksMarkers.getMarker(bookmark.getId());
        
        return marker != null;
    }

    @Override
    protected void bookmarkModified(Bookmark oldBookmark, Bookmark newBookmark) {
        ApplicationManager.getApplication().invokeLater(this::updateMarkerInfo);
    }

    /**
     * Custom component that displays marker information with icon, name, line number and relative path
     */
    private static class MarkerInfoRenderer extends SimpleColoredComponent {
        private final Project project;

        public MarkerInfoRenderer(Project project) {
            super();
            this.project = project;
        }

        public void setMarker(BookmarkMarker marker) {
            clear();

            if (marker == null) {
                setIcon(com.intellij.icons.AllIcons.General.Warning);
                append("Marker deleted", SimpleTextAttributes.ERROR_ATTRIBUTES);
                setToolTipText("The marker associated with this bookmark has been deleted");
                return;
            }

            VirtualFile file = marker.getResource();
            int lineNumber = marker.getLineNumber();

            // Set file icon
            Icon icon = IconUtil.getIcon(file, 0, project);
            setIcon(icon);

            // File name with line number
            String fileName = file.getName();
            append(String.format("%s (line %d)", fileName, lineNumber + 1), SimpleTextAttributes.LINK_ATTRIBUTES);

            // Relative path in gray
            String relativePath = getRelativePath(file);
            if (relativePath != null && !relativePath.equals(fileName)) {
                append(" - " + relativePath, SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }

            // Set tooltip with full path
            setToolTipText(file.getPath());
        }

        @Override
        public void clear() {
            super.clear();
            setIcon(null);
            setToolTipText(null);
        }

        private String getRelativePath(VirtualFile file) {
            ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
            VirtualFile contentRoot = projectFileIndex.getContentRootForFile(file);

            if (contentRoot != null) {
                String fullPath = file.getPath();
                String contentRootPath = contentRoot.getPath();
                if (fullPath.startsWith(contentRootPath)) {
                    return fullPath.substring(contentRootPath.length() + 1);
                }
            }

            return file.getPath();
        }
    }
}
