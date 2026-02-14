package mesfavoris.internal.ui.details;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.bookmarktype.IFileBookmarkLocation;
import mesfavoris.internal.bookmarktypes.extension.ExtensionBookmarkLocationProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class PreviewBookmarkDetailPart extends AbstractBookmarkDetailPart {
    private JPanel mainPanel;
    private Editor currentEditor;
    private final IBookmarkLocationProvider locationProvider;

    public PreviewBookmarkDetailPart(Project project) {
        this(project, project.getService(IBookmarksService.class).getBookmarkDatabase());
    }

    public PreviewBookmarkDetailPart(Project project, BookmarkDatabase bookmarkDatabase) {
        super(project, bookmarkDatabase);
        this.locationProvider = new ExtensionBookmarkLocationProvider();
    }

    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new BorderLayout());
        return mainPanel;
    }

    private void releaseCurrentEditor() {
        if (currentEditor != null) {
            EditorFactory.getInstance().releaseEditor(currentEditor);
            currentEditor = null;
        }
        mainPanel.removeAll();
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void applyEditorSettings(EditorSettings settings) {
        settings.setLineNumbersShown(true);
        settings.setFoldingOutlineShown(true);
        settings.setLineMarkerAreaShown(true);
        settings.setIndentGuidesShown(false);
        settings.setVirtualSpace(false);
        settings.setAdditionalLinesCount(0);
        settings.setAdditionalPageAtBottom(false);
    }

    @Override
    public void setBookmark(@Nullable Bookmark bookmark) {
        super.setBookmark(bookmark);
        updatePreview();
    }

    @Override
    protected void bookmarkModified(Bookmark oldBookmark, @Nullable Bookmark newBookmark) {
        updatePreview();
    }

    private void updatePreview() {
        if (bookmark == null) {
            clearPreview();
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            IBookmarkLocation location;
            try {
                location = locationProvider.getBookmarkLocation(project, bookmark, new EmptyProgressIndicator());
            } catch (Exception e) {
                location = null;
            }

            final IBookmarkLocation finalLocation = location;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (finalLocation instanceof IFileBookmarkLocation fileBookmarkLocation) {
                    VirtualFile file = fileBookmarkLocation.getFile();
                    Integer line = fileBookmarkLocation.getLineNumber();

                    if (file != null && file.isValid()) {
                        showFilePreview(file, line);
                    } else {
                        clearPreview();
                    }
                } else {
                    clearPreview();
                }
            });
        });
    }

    private void showFilePreview(VirtualFile file, @Nullable Integer line) {
        releaseCurrentEditor();

        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            // Create a read-only viewer
            currentEditor = EditorFactory.getInstance().createViewer(document, project);
            applyEditorSettings(currentEditor.getSettings());
            
            mainPanel.add(currentEditor.getComponent(), BorderLayout.CENTER);
            
            if (line != null && line >= 0) {
                int lineIndex = line;
                if (lineIndex < document.getLineCount()) {
                    // Execute later to ensure editor component is laid out
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (currentEditor != null && !currentEditor.isDisposed()) {
                            currentEditor.getCaretModel().moveToLogicalPosition(new LogicalPosition(lineIndex, 0));
                            currentEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
                        }
                    });
                }
            }
        } else {
            addPlaceholderLabel("<Preview not available for this file type>");
        }
        mainPanel.revalidate();
    }

    private void clearPreview() {
        releaseCurrentEditor();
        addPlaceholderLabel("");
    }

    private void addPlaceholderLabel(String text) {
        mainPanel.removeAll();
        if (!text.isEmpty()) {
            mainPanel.add(new JLabel(text, SwingConstants.CENTER), BorderLayout.CENTER);
        }
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    @Override
    public void dispose() {
        releaseCurrentEditor();
        super.dispose();
    }

    @Override
    public boolean canHandle(@Nullable Bookmark bookmark) {
        return true;
    }

    @Override
    public String getTitle() {
        return "Preview";
    }
}
