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
import mesfavoris.model.Bookmark;
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
    private FileInfoRenderer fileInfoRenderer;
    private BookmarkMarker currentMarker;
    private final IBookmarksMarkers bookmarksMarkers;

    public MarkerBookmarkDetailPart(Project project) {
        super(project);
        BookmarksService bookmarksService = project.getService(BookmarksService.class);
        this.bookmarksMarkers = bookmarksService.getBookmarksMarkers();
    }

    @Override
    public String getTitle() {
        return "Marker";
    }

    @Override
    public JComponent createComponent() {
        JBPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));

        fileInfoRenderer = new FileInfoRenderer();
        fileInfoRenderer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fileInfoRenderer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentMarker != null) {
                    openFileAtMarker();
                }
            }
        });

        panel.add(fileInfoRenderer, BorderLayout.NORTH);
        return panel;
    }

    @Override
    public void setBookmark(Bookmark bookmark) {
        super.setBookmark(bookmark);
        updateMarkerInfo();
    }

    private void updateMarkerInfo() {
        if (bookmark == null) {
            currentMarker = null;
            fileInfoRenderer.clear();
            return;
        }
        currentMarker = bookmarksMarkers.getMarker(bookmark.getId());
        if (currentMarker != null) {
            VirtualFile file = currentMarker.getResource();
            int lineNumber = getLineNumberFromMarker(currentMarker);
            fileInfoRenderer.setFileInfo(file, lineNumber + 1); // +1 for 1-based display
        } else {
            fileInfoRenderer.clear();
        }
    }

    private void openFileAtMarker() {
        if (currentMarker == null) {
            return;
        }

        VirtualFile file = currentMarker.getResource();
        int lineNumber = getLineNumberFromMarker(currentMarker);

        // Use OpenFileDescriptor to open the file at the specific line
        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, lineNumber, 0);
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
    }

    private int getLineNumberFromMarker(BookmarkMarker marker) {
        String lineNumberStr = marker.getAttributes().get(BookmarkMarker.LINE_NUMBER);
        if (lineNumberStr != null) {
            try {
                return Integer.parseInt(lineNumberStr);
            } catch (NumberFormatException e) {
                // ignore, use 0
            }
        }
        return 0;
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
     * Custom component that displays file information with icon, name, line number and relative path
     */
    private class FileInfoRenderer extends SimpleColoredComponent {

        public void setFileInfo(VirtualFile file, int lineNumber) {
            clear();

            // Set file icon
            Icon icon = IconUtil.getIcon(file, 0, project);
            setIcon(icon);

            // File name with line number
            String fileName = file.getName();
            append(String.format("%s (line %d)", fileName, lineNumber), SimpleTextAttributes.LINK_ATTRIBUTES);

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
