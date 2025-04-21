package mesfavoris.internal.ui.details;

import static com.intellij.util.containers.ContainerUtil.addIfNotNull;
import static mesfavoris.model.Bookmark.PROPERTY_COMMENT;

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
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Create component to display bookmarks comments
 * 
 * @author cchabanois
 *
 */
public class CommentBookmarkDetailPart extends AbstractBookmarkDetailPart {
    private EditorTextField editorField;
    private final BookmarkDatabase bookmarkDatabase;

    private static final @NotNull EditorCustomization COLOR_SCHEME_FOR_CURRENT_UI_THEME_CUSTOMIZATION = editor -> {
        editor.setBackgroundColor(null); // to use background from set color scheme
        editor.setColorsScheme(getBookmarkCommentColorScheme(editor));
    };

    public CommentBookmarkDetailPart(Project project, BookmarkDatabase bookmarkDatabase) {
        super(project);
        this.bookmarkDatabase = bookmarkDatabase;
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
                final String newComment = event.getDocument().getText();
                try {
                    bookmarkDatabase.modify(bookmarksTreeModifier -> {
                        if (newComment.isEmpty()) {
                            bookmarksTreeModifier.setPropertyValue(bookmark.getId(), PROPERTY_COMMENT, null);
                        } else {
                            bookmarksTreeModifier.setPropertyValue(bookmark.getId(), PROPERTY_COMMENT, newComment);
                        }
                    }, (bookmarksTree) -> bookmark = bookmarksTree.getBookmark(bookmark.getId()));
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

        return colorsScheme;
    }

	@Override
	public void setBookmark(Bookmark bookmark) {
		String oldComment = this.bookmark != null ? this.bookmark.getPropertyValue(PROPERTY_COMMENT): null;
        super.setBookmark(bookmark);
        String newComment = this.bookmark != null ? this.bookmark.getPropertyValue(PROPERTY_COMMENT): null;
        if (!Objects.equals(newComment, oldComment)) {
            editorField.setText(newComment);
        }
	}

	@Override
	public boolean canHandle(Bookmark bookmark) {
		// all bookmarks can have comments
		return true;
	}

	@Override
	public String getTitle() {
		return "Comments";
	}

	@Override
	protected void bookmarkModified(Bookmark oldBookmark, Bookmark newBookmark) {
        ApplicationManager.getApplication().invokeLater(() -> setBookmark(newBookmark));
	}

}
