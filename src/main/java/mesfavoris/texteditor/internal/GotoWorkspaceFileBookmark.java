package mesfavoris.texteditor.internal;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IGotoBookmark;
import mesfavoris.icons.MesFavorisIcons;
import mesfavoris.model.Bookmark;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class GotoWorkspaceFileBookmark implements IGotoBookmark {

	@Override
	public boolean gotoBookmark(Project project, Bookmark bookmark, IBookmarkLocation bookmarkLocation) {
		if (!(bookmarkLocation instanceof WorkspaceFileBookmarkLocation)) {
			return false;
		}
		WorkspaceFileBookmarkLocation workspaceFileBookmarkLocation = (WorkspaceFileBookmarkLocation) bookmarkLocation;
		VirtualFile file = workspaceFileBookmarkLocation.getWorkspaceFile();
		Editor editor = openEditor(project, file);
		if (editor == null) {
			return false;
		}
		if (workspaceFileBookmarkLocation.getLineNumber() != null) {
			gotoLine(editor, workspaceFileBookmarkLocation.getLineNumber());
		}

/*

		Document document = FileDocumentManager.getInstance().getDocument(file);
		MarkupModelEx markup = (MarkupModelEx) DocumentMarkupModel.forDocument(document, project, true);

		RangeHighlighterEx highlighter = markup.addPersistentLineHighlighter(CodeInsightColors.BOOKMARKS_ATTRIBUTES, workspaceFileBookmarkLocation.getLineNumber(), HighlighterLayer.ERROR + 1);
		if (highlighter != null) {
			highlighter.setGutterIconRenderer(new GutterIconRenderer() {
				@Override
				public boolean equals(Object obj) {
					return false;
				}

				@Override
				public int hashCode() {
					return 0;
				}

				@Override
				public @NotNull Icon getIcon() {
					return MesFavorisIcons.bookmark;
				}
			});
//			highlighter.setErrorStripeTooltip(getBookmarkTooltip());
		}
*/
		return true;
	}

	private void gotoLine(Editor editor, int lineNumber) {
		LogicalPosition position = new LogicalPosition(lineNumber, 0);
		editor.getCaretModel().removeSecondaryCarets();
		editor.getCaretModel().moveToLogicalPosition(position);
		editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
		editor.getSelectionModel().removeSelection();
		IdeFocusManager.getGlobalInstance().requestFocus(editor.getContentComponent(), true);
	}

	private Editor openEditor(Project project, VirtualFile file) {
		if (!file.exists()) {
			return null;
		}
		Editor editor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, file), true);
		return editor;
	}

}
