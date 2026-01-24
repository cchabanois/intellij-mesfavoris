package mesfavoris.texteditor.internal;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IGotoBookmark;
import mesfavoris.model.Bookmark;

public class GotoWorkspaceFileBookmark implements IGotoBookmark {

	@Override
	public boolean gotoBookmark(Project project, Bookmark bookmark, IBookmarkLocation bookmarkLocation) {
		if (!(bookmarkLocation instanceof WorkspaceFileBookmarkLocation workspaceFileBookmarkLocation)) {
			return false;
		}
		VirtualFile file = workspaceFileBookmarkLocation.getFile();
		Editor editor = openEditor(project, file);
		if (editor == null) {
			return false;
		}
		if (workspaceFileBookmarkLocation.getLineNumber() != null) {
			gotoLine(editor, workspaceFileBookmarkLocation.getLineNumber());
		}
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
