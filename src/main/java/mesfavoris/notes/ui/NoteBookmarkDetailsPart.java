package mesfavoris.notes.ui;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.TextEditorWithPreview;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LightVirtualFile;
import mesfavoris.BookmarksException;
import mesfavoris.internal.ui.details.AbstractBookmarkDetailPart;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.notes.NoteBookmarkProperties;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

public class NoteBookmarkDetailsPart extends AbstractBookmarkDetailPart {

    private static final String SPLIT_PROVIDER_TEXT_EDITOR_MARKDOWN_PREVIEW_EDITOR = "split-provider[text-editor;markdown-preview-editor]";
    private TextEditorWithPreview textEditorWithPreview;

    private boolean updatingText = false;

    public NoteBookmarkDetailsPart(Project project) {
        this(project, project.getService(IBookmarksService.class).getBookmarkDatabase());
    }

    public NoteBookmarkDetailsPart(Project project, BookmarkDatabase bookmarkDatabase) {
        super(project, bookmarkDatabase);
    }

    @Override
    public JComponent createComponent() {
        return getMarkdownFileEditorProvider().map(provider -> {
            LightVirtualFile virtualFile = new LightVirtualFile("note.md", "");
            textEditorWithPreview = (TextEditorWithPreview) provider.createEditor(project, virtualFile);
            Disposer.register(this, textEditorWithPreview);

            TextEditor textEditor = textEditorWithPreview.getTextEditor();
            textEditor.getEditor().getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    if (updatingText) {
                        return;
                    }
                    try {
                        TextEditor textEditor = textEditorWithPreview.getTextEditor();
                        String newContent = textEditor.getEditor().getDocument().getText();
                        bookmarkDatabase.modify(modifier -> modifier.setPropertyValue(bookmark.getId(), NoteBookmarkProperties.PROP_NOTES, newContent));
                    } catch (BookmarksException e) {
                        // never happen
                    }
                }
            }, textEditorWithPreview);
            return textEditorWithPreview.getComponent();
        }).orElseGet(() -> new JLabel("Markdown editor provider not found."));
    }

    @Override
    public void setBookmark(Bookmark bookmark) {
        super.setBookmark(bookmark);
        String noteContent = (this.bookmark != null) ? this.bookmark.getPropertyValue(NoteBookmarkProperties.PROP_NOTES) : null;
        setText(noteContent);
        textEditorWithPreview.getComponent().setEnabled(this.bookmark != null && bookmarkDatabase.getBookmarksModificationValidator()
                .validateModification(bookmarkDatabase.getBookmarksTree(), bookmark.getId())
                .isOk());
    }

    @Override
    public void dispose() {
        textEditorWithPreview = null;
        super.dispose();
    }

    @Override
    public boolean canHandle(Bookmark bookmark) {
        return bookmark != null && bookmark.getPropertyValue(NoteBookmarkProperties.PROP_NOTES) != null;
    }

    @Override
    public String getTitle() {
        return "Note";
    }

    private void setText(String text) {
        updatingText = true;
        try {
            TextEditor textEditor = textEditorWithPreview.getTextEditor();
            Document document = textEditor.getEditor().getDocument();
            WriteAction.run(() -> document.setText(text != null ? text : ""));
        } finally {
            updatingText = false;
        }
    }

    @Override
    protected void bookmarkModified(Bookmark oldBookmark, Bookmark newBookmark) {
        String oldContent = oldBookmark != null ? oldBookmark.getPropertyValue(NoteBookmarkProperties.PROP_NOTES) : "";
        String newContent = newBookmark.getPropertyValue(NoteBookmarkProperties.PROP_NOTES);
        if (!Objects.equals(oldContent, newContent)) {
            setText(newContent);
        }
    }

    private Optional<FileEditorProvider> getMarkdownFileEditorProvider() {
        return java.util.Arrays.stream(FileEditorProvider.EP_FILE_EDITOR_PROVIDER.getExtensions())
            .filter(p -> SPLIT_PROVIDER_TEXT_EDITOR_MARKDOWN_PREVIEW_EDITOR.equals(p.getEditorTypeId()))
            .findFirst();
    }
}
