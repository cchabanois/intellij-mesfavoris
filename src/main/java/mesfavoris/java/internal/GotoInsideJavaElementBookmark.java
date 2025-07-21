package mesfavoris.java.internal;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IGotoBookmark;
import mesfavoris.model.Bookmark;

public class GotoInsideJavaElementBookmark implements IGotoBookmark {

	@Override
	public boolean gotoBookmark(Project project, Bookmark bookmark, IBookmarkLocation bookmarkLocation) {
		if (!(bookmarkLocation instanceof JavaTypeMemberBookmarkLocation)) {
			return false;
		}
		JavaTypeMemberBookmarkLocation location = (JavaTypeMemberBookmarkLocation) bookmarkLocation;
		Editor editor = openInEditor(project, location.getMember());
		if (editor == null) {
			return false;
		}
		if (location.getLineNumber() != null) {
			gotoLine(editor, location.getLineNumber());
		}
		return true;
	}

	private Editor openInEditor(Project project, PsiMember psiMember) {
		PsiFile containingFile = psiMember.getContainingFile();
		if (containingFile == null) {
			return null;
		}

		VirtualFile virtualFile = containingFile.getVirtualFile();
		if (virtualFile == null) {
			return null;
		}

		// Navigate to the element using IntelliJ's navigation support
		if (psiMember.canNavigate()) {
			psiMember.navigate(true);
		}

		// Open the file in editor and return the editor instance
		return FileEditorManager.getInstance(project).openTextEditor(
			new OpenFileDescriptor(project, virtualFile, psiMember.getTextOffset()), true);
	}

	private void gotoLine(Editor editor, int lineNumber) {
		LogicalPosition position = new LogicalPosition(lineNumber, 0);
		editor.getCaretModel().removeSecondaryCarets();
		editor.getCaretModel().moveToLogicalPosition(position);
		editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
		editor.getSelectionModel().removeSelection();
		IdeFocusManager.getGlobalInstance().requestFocus(editor.getContentComponent(), true);
	}

}
