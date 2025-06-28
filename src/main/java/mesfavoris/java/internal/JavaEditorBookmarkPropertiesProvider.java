package mesfavoris.java.internal;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import mesfavoris.bookmarktype.AbstractBookmarkPropertiesProvider;
import mesfavoris.texteditor.TextEditorUtils;

import java.util.Map;

import static mesfavoris.java.JavaBookmarkProperties.*;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_LINE_CONTENT;

public class JavaEditorBookmarkPropertiesProvider extends AbstractBookmarkPropertiesProvider {

	@Override
	public void addBookmarkProperties(Map<String, String> bookmarkProperties, DataContext dataContext, ProgressIndicator progress) {
		try {
			// ReadAction needed for PSI access
			ReadAction.run(() -> {
				Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
				Project project = dataContext.getData(CommonDataKeys.PROJECT);
				VirtualFile virtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE);

				if (editor == null || project == null || virtualFile == null) {
					return;
				}

				PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
				if (!(psiFile instanceof PsiJavaFile psiJavaFile)) {
					return;
				}

				int lineNumber = getCurrentLineNumber(editor);
				int offset = JavaEditorUtils.getOffsetOfFirstNonWhitespaceCharAtLine(editor, lineNumber);

				PsiMember member = JavaEditorUtils.getJavaElementAt(psiJavaFile, offset);
				if (member == null) {
					return;
				}
				addMemberBookmarkProperties(bookmarkProperties, member);
				addLineNumberInsideMemberProperty(bookmarkProperties, member, lineNumber);
				addJavadocComment(bookmarkProperties, member, lineNumber);
				addLineContent(bookmarkProperties, editor, lineNumber);
			});
		} catch (Exception e) {
			// Silently ignore errors in tests or when PSI is not available
		}
	}

	private int getCurrentLineNumber(Editor editor) {
		return editor.offsetToLogicalPosition(editor.getSelectionModel().getSelectionStart()).line;
	}

	private void addLineNumberInsideMemberProperty(Map<String, String> bookmarkProperties, PsiMember member,
			int lineNumber) {
		int memberLineNumber = JavaEditorUtils.getLineNumber(member);
		if (memberLineNumber >= 0) {
			int lineNumberInsideMember = lineNumber - memberLineNumber;
			putIfAbsent(bookmarkProperties, PROP_LINE_NUMBER_INSIDE_ELEMENT, Integer.toString(lineNumberInsideMember));
		}
	}

	private void addJavadocComment(Map<String, String> bookmarkProperties, PsiMember member,
			int lineNumber) {
		if (JavaEditorUtils.getLineNumber(member) != lineNumber) {
			return;
		}
		String javadoc = JavadocCommentProvider.getJavadocCommentAsText(member);
		if (javadoc != null) {
			putIfAbsent(bookmarkProperties, PROPERTY_COMMENT, javadoc);
		}
	}

	private void addLineContent(Map<String, String> properties, Editor editor, int lineNumber) {
		putIfAbsent(properties, PROP_LINE_CONTENT, () -> {
			String content = TextEditorUtils.getLineContent(editor.getDocument(), lineNumber);
			return content == null ? null : content.trim();
		});
	}

	protected void addMemberBookmarkProperties(Map<String, String> bookmarkProperties, PsiMember member) {
		putIfAbsent(bookmarkProperties, PROP_JAVA_ELEMENT_NAME, member.getName());

		PsiClass containingClass = member.getContainingClass();
		if (containingClass != null) {
			String qualifiedName = containingClass.getQualifiedName();
			putIfAbsent(bookmarkProperties, PROP_JAVA_DECLARING_TYPE, qualifiedName);
		}

		putIfAbsent(bookmarkProperties, PROP_JAVA_ELEMENT_KIND, getKind(member));

		if (member instanceof PsiMethod method) {
            putIfAbsent(bookmarkProperties, PROP_JAVA_METHOD_SIGNATURE,
					JavaEditorUtils.getMethodSimpleSignature(method));
			putIfAbsent(bookmarkProperties, PROPERTY_NAME,
					bookmarkProperties.get(PROP_JAVA_DECLARING_TYPE) + '.' + member.getName() + "()");
		}

		if (member instanceof PsiClass type) {
            String qualifiedName = type.getQualifiedName();
			putIfAbsent(bookmarkProperties, PROP_JAVA_TYPE, qualifiedName);
			putIfAbsent(bookmarkProperties, PROPERTY_NAME, qualifiedName);
		}

		if (member instanceof PsiField) {
			putIfAbsent(bookmarkProperties, PROPERTY_NAME,
					bookmarkProperties.get(PROP_JAVA_DECLARING_TYPE) + '.' + member.getName());
		}

		// Fallback name
		putIfAbsent(bookmarkProperties, PROPERTY_NAME, member.getName());
	}

	private String getKind(PsiMember member) {
		if (member instanceof PsiMethod) {
			return KIND_METHOD;
		}
		if (member instanceof PsiField) {
			return KIND_FIELD;
		}
		if (member instanceof PsiClass psiClass) {
            if (psiClass.isAnnotationType()) {
				return KIND_ANNOTATION;
			}
			if (psiClass.isInterface()) {
				return KIND_INTERFACE;
			}
			if (psiClass.isEnum()) {
				return KIND_ENUM;
			}
			return KIND_CLASS;
		}
		if (member instanceof PsiClassInitializer) {
			return KIND_INITIALIZER;
		}
		return KIND_TYPE;
	}

}
