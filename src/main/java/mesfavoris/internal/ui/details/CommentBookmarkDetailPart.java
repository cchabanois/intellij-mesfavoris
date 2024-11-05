package mesfavoris.internal.ui.details;

import static com.intellij.util.containers.ContainerUtil.addIfNotNull;
import static mesfavoris.model.Bookmark.PROPERTY_COMMENT;

import com.intellij.ide.ui.UISettingsUtils;
import com.intellij.openapi.editor.SpellCheckingEditorCustomizationProvider;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.ui.*;
import com.intellij.util.ui.UIUtil;
import mesfavoris.model.Bookmark;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Create component to display bookmarks comments
 * 
 * @author cchabanois
 *
 */
public class CommentBookmarkDetailPart extends AbstractBookmarkDetailPart {
    private EditorTextField editorField;

    private static final @NotNull EditorCustomization COLOR_SCHEME_FOR_CURRENT_UI_THEME_CUSTOMIZATION = editor -> {
        editor.setBackgroundColor(null); // to use background from set color scheme
        editor.setColorsScheme(getBookmarkCommentColorScheme(editor));
    };

    public CommentBookmarkDetailPart(Project project) {
        super(project);
    }

    @Override
    public JComponent createComponent() {
        super.createComponent();
        Set<EditorCustomization> features = new HashSet<>();
        features.add(SoftWrapsEditorCustomization.ENABLED);
        features.add(AdditionalPageAtBottomEditorCustomization.DISABLED);
        features.add(COLOR_SCHEME_FOR_CURRENT_UI_THEME_CUSTOMIZATION);
        addIfNotNull(features, SpellCheckingEditorCustomizationProvider.getInstance().getEnabledCustomization());

        this.editorField =
                EditorTextFieldProvider.getInstance().getEditorField(FileTypes.PLAIN_TEXT.getLanguage(), project, features);

        // Global editor color scheme is set by EditorTextField logic. We also need to use font from it and not from the current LaF.
        this.editorField.setFontInheritedFromLAF(false);
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
		super.setBookmark(bookmark);
        editorField.setText(bookmark.getPropertyValue(PROPERTY_COMMENT));
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
/*		if (newBookmark == null || !Objects.equal(bookmarkCommentArea.getBookmark().getPropertyValue(PROPERTY_COMMENT),
				newBookmark.getPropertyValue(PROPERTY_COMMENT))) {
			Display.getDefault().asyncExec(() -> setBookmark(newBookmark));
		} */
	}

}
