package mesfavoris.notes.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.TextEditorWithPreview;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBPanel;
import mesfavoris.BookmarksException;
import mesfavoris.internal.ui.details.AbstractBookmarkDetailPart;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.notes.NoteBookmarkProperties;
import mesfavoris.service.IBookmarksService;
import org.intellij.plugins.markdown.ui.preview.MarkdownSplitEditorProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class NoteDetailsPart extends AbstractBookmarkDetailPart {

    private final JPanel mainPanel;
    private FileEditor currentEditor;
    private DocumentListener documentListener;
    private boolean isUpdating = false;

    public NoteDetailsPart(Project project) {
        this(project, project.getService(IBookmarksService.class).getBookmarkDatabase());
    }

    public NoteDetailsPart(Project project, BookmarkDatabase bookmarkDatabase) {
        super(project, bookmarkDatabase);
        this.mainPanel = new JBPanel<>(new BorderLayout());
    }

    @Override
    public JComponent createComponent() {
        return mainPanel;
    }

    @Override
    public void setBookmark(Bookmark bookmark) {
        super.setBookmark(bookmark);
        updateView();
    }

    @Override
    protected void bookmarkModified(Bookmark oldBookmark, Bookmark newBookmark) {
        if (newBookmark != null && newBookmark.equals(this.bookmark)) {
            String oldContent = oldBookmark != null ? oldBookmark.getPropertyValue(NoteBookmarkProperties.PROP_NOTE_CONTENT, "") : "";
            String newContent = newBookmark.getPropertyValue(NoteBookmarkProperties.PROP_NOTE_CONTENT, "");
            if (!Objects.equals(oldContent, newContent)) {
                updateText(newContent);
            }
        }
    }

    private void updateView() {
        if (bookmark == null) {
            releaseCurrentEditor();
            mainPanel.setVisible(false);
            return;
        }

        mainPanel.setVisible(true);

        if (currentEditor == null) {
            createEditor();
        }

        String noteContent = bookmark.getPropertyValue(NoteBookmarkProperties.PROP_NOTES);
        updateText(noteContent);
    }
    
    private void updateText(String text) {
        if (currentEditor == null || isUpdating) {
            return;
        }

        isUpdating = true;
        try {
            TextEditor textEditor = ((TextEditorWithPreview) currentEditor).getTextEditor();
            Document document = textEditor.getEditor().getDocument();
            if (!document.getText().equals(text)) {
                WriteAction.run(() -> document.setText(text != null ? text : ""));
            }
        } finally {
            isUpdating = false;
        }
    }


    private void createEditor() {
        releaseCurrentEditor();

        LightVirtualFile virtualFile = new LightVirtualFile("note.md", "");

        MarkdownSplitEditorProvider provider = FileEditorProvider.EP_FILE_EDITOR_PROVIDER.findExtension(MarkdownSplitEditorProvider.class);
        currentEditor = provider.createEditor(project, virtualFile);

        if (currentEditor instanceof TextEditorWithPreview) {
            mainPanel.add(currentEditor.getComponent(), BorderLayout.CENTER);

            TextEditor textEditor = ((TextEditorWithPreview) currentEditor).getTextEditor();
            documentListener = new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    if (!isUpdating) {
                        saveContent();
                    }
                }
            };
            textEditor.getEditor().getDocument().addDocumentListener(documentListener);
        }

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void saveContent() {
        if (bookmark == null || !(currentEditor instanceof TextEditorWithPreview) || !currentEditor.isValid()) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                TextEditor textEditor = ((TextEditorWithPreview) currentEditor).getTextEditor();
                String newContent = textEditor.getEditor().getDocument().getText();

                String currentContentInDb = bookmarkDatabase.getBookmarksTree()
                        .getBookmark(bookmark.getId())
                        .getPropertyValue(NoteBookmarkProperties.PROP_NOTES);
                
                if (newContent.equals(currentContentInDb)) {
                    return; 
                }

                bookmarkDatabase.modify(modifier -> modifier.setPropertyValue(bookmark.getId(), NoteBookmarkProperties.PROP_NOTES, newContent));
            } catch (BookmarksException ignored) {
            }
        });
    }

    private void releaseCurrentEditor() {
        if (currentEditor != null) {
            if (documentListener != null && currentEditor instanceof TextEditorWithPreview) {
                try {
                    TextEditor textEditor = ((TextEditorWithPreview) currentEditor).getTextEditor();
                    textEditor.getEditor().getDocument().removeDocumentListener(documentListener);
                } catch (Exception ignored) {
                }
            }
            mainPanel.removeAll();
            currentEditor.dispose();
            currentEditor = null;
            documentListener = null;
        }
    }

    @Override
    public void dispose() {
        releaseCurrentEditor();
        super.dispose();
    }

    @Override
    public boolean canHandle(Bookmark bookmark) {
        return bookmark.getPropertyValue(NoteBookmarkProperties.PROP_NOTES) != null;
    }

    @Override
    public String getTitle() {
        return "Note";
    }
}