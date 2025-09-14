package mesfavoris.snippets.internal;

import com.intellij.ide.ui.UISettingsUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.SpellCheckingEditorCustomizationProvider;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.ui.*;
import com.intellij.util.ui.UIUtil;
import mesfavoris.BookmarksException;
import mesfavoris.internal.ui.details.AbstractBookmarkDetailPart;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.intellij.util.containers.ContainerUtil.addIfNotNull;
import static mesfavoris.snippets.SnippetBookmarkProperties.PROP_SNIPPET_CONTENT;

/**
 * Create component to display bookmark snippet
 * 
 * @author cchabanois
 *
 */
public class SnippetBookmarkDetailPart extends AbstractBookmarkDetailPart {
    private EditorTextField editorField;
    private final BookmarkDatabase bookmarkDatabase;

    private static final @NotNull EditorCustomization COLOR_SCHEME_FOR_CURRENT_UI_THEME_CUSTOMIZATION = editor -> {
        editor.setBackgroundColor(null); // to use background from set color scheme
        editor.setColorsScheme(getBookmarkCommentColorScheme(editor));
    };

    public SnippetBookmarkDetailPart(Project project) {
        super(project);
        this.bookmarkDatabase = project.getService(BookmarksService.class).getBookmarkDatabase();
    }

    @Override
    public JComponent createComponent() {
        Set<EditorCustomization> features = new HashSet<>();
        features.add(SoftWrapsEditorCustomization.ENABLED);
        features.add(AdditionalPageAtBottomEditorCustomization.DISABLED);
        features.add(COLOR_SCHEME_FOR_CURRENT_UI_THEME_CUSTOMIZATION);
        addIfNotNull(features, SpellCheckingEditorCustomizationProvider.getInstance().getEnabledCustomization());

        this.editorField =
                EditorTextFieldProvider.getInstance().getEditorField(FileTypes.PLAIN_TEXT.getLanguage(), project, features);

        // Global editor color scheme is set by EditorTextField logic. We also need to use font from it and not from the current LaF.
        this.editorField.setFontInheritedFromLAF(false);
        editorField.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                final String newSnippet = event.getDocument().getText();
                try {
                    bookmarkDatabase.modify(bookmarksTreeModifier ->
                            bookmarksTreeModifier.setPropertyValue(bookmark.getId(), PROP_SNIPPET_CONTENT, newSnippet),
                            (bookmarksTree) -> bookmark = bookmarksTree.getBookmark(bookmark.getId()));
                } catch (BookmarksException e) {
                    // never happen
                }
            }
        });
        return this.editorField;

    }

    @NotNull
    private static EditorColorsScheme getBookmarkCommentColorScheme(EditorEx editor) {
        boolean isLaFDark = ColorUtil.isDark(UIUtil.getPanelBackground());
        boolean isEditorDark = EditorColorsManager.getInstance().isDarkEditor();
        EditorColorsScheme colorsScheme = isLaFDark == isEditorDark
                ? EditorColorsManager.getInstance().getGlobalScheme()
                : EditorColorsManager.getInstance().getSchemeForCurrentUITheme();

        // We have to wrap the colorsScheme into a scheme delegate in order to avoid editing the global scheme
        colorsScheme = editor.createBoundColorSchemeDelegate(colorsScheme);
        colorsScheme.setEditorFontSize(UISettingsUtils.getInstance().getScaledEditorFontSize());
        Font editorFont = new Font(Font.MONOSPACED, Font.PLAIN, colorsScheme.getEditorFontSize());
        colorsScheme.setEditorFontName(editorFont.getName());

        return colorsScheme;
    }

	@Override
	public void setBookmark(Bookmark bookmark) {
        super.setBookmark(bookmark);
        String newComment = this.bookmark != null ? this.bookmark.getPropertyValue(PROP_SNIPPET_CONTENT) : null;
        editorField.setText(newComment);
	}

	@Override
	public boolean canHandle(Bookmark bookmark) {
		return bookmark.getPropertyValue(PROP_SNIPPET_CONTENT) != null;
	}

	@Override
	public String getTitle() {
		return "Snippet";
	}

	@Override
	protected void bookmarkModified(Bookmark oldBookmark, Bookmark newBookmark) {
        String oldSnippet = oldBookmark.getPropertyValue(PROP_SNIPPET_CONTENT);
        String newSnippet = newBookmark.getPropertyValue(PROP_SNIPPET_CONTENT);
        if (!Objects.equals(newSnippet, oldSnippet)) {
            ApplicationManager.getApplication().invokeLater(() -> editorField.setText(newSnippet));
        }
	}

}
