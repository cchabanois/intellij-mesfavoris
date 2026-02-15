package mesfavoris.texteditor.internal;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.concurrency.AppExecutorUtil;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.bookmarktype.IFileBookmarkLocation;
import mesfavoris.internal.bookmarktypes.extension.ExtensionBookmarkLocationProvider;
import mesfavoris.internal.ui.details.AbstractBookmarkDetailPart;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

import static com.intellij.openapi.editor.markup.HighlighterLayer.SELECTION;
import static mesfavoris.java.JavaBookmarkProperties.PROP_JAVA_ELEMENT_NAME;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_FILE_PATH;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_WORKSPACE_PATH;

public class TextEditorPreviewBookmarkDetailPart extends AbstractBookmarkDetailPart {
    private JPanel mainPanel;
    private Editor currentEditor;
    private final IBookmarkLocationProvider locationProvider;

    public TextEditorPreviewBookmarkDetailPart(Project project) {
        this(project, project.getService(IBookmarksService.class).getBookmarkDatabase());
    }

    public TextEditorPreviewBookmarkDetailPart(Project project, BookmarkDatabase bookmarkDatabase) {
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
        if (mainPanel != null) {
            mainPanel.removeAll();
            mainPanel.revalidate();
            mainPanel.repaint();
        }
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

        ReadAction.nonBlocking(() -> {
            try {
                return locationProvider.getBookmarkLocation(project, bookmark, ProgressManager.getInstance().getProgressIndicator());
            } catch (Exception e) {
                return null;
            }
        })
        .coalesceBy(this)
        .expireWith(this)
        .finishOnUiThread(ModalityState.defaultModalityState(), location -> {
            if (location instanceof IFileBookmarkLocation fileBookmarkLocation) {
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
        })
        .submit(AppExecutorUtil.getAppExecutorService());
    }

    private void showFilePreview(VirtualFile file, @Nullable Integer line) {
        releaseCurrentEditor();

        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            // Create a read-only viewer
            currentEditor = EditorFactory.getInstance().createViewer(document, project);
            if (currentEditor instanceof EditorEx currentEditorEx) {
                EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file);
                currentEditorEx.setHighlighter(highlighter);
            }
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
                            
                            // Highlight the line
                            MarkupModel markupModel = currentEditor.getMarkupModel();
                            TextAttributes attributes = new TextAttributes();
                            // Light orange for light theme, Dark Brownish-Orange for dark theme
                            attributes.setBackgroundColor(new JBColor(new Color(255, 220, 180), new Color(85, 55, 20))); 
                            markupModel.addLineHighlighter(lineIndex, SELECTION - 1, attributes);
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
        if (bookmark == null) {
            return false;
        }
        return bookmark.getPropertyValue(PROP_WORKSPACE_PATH) != null ||
               bookmark.getPropertyValue(PROP_FILE_PATH) != null ||
               bookmark.getPropertyValue(PROP_JAVA_ELEMENT_NAME) != null;
    }

    @Override
    public String getTitle() {
        return "Preview";
    }
}
