package mesfavoris.java.internal;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.java.internal.editor.JavaEditorUtils;
import mesfavoris.model.Bookmark;
import mesfavoris.texteditor.TextEditorBookmarkProperties;
import mesfavoris.texteditor.text.matching.DocumentFuzzySearcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static mesfavoris.java.JavaBookmarkProperties.*;

public class JavaTypeMemberBookmarkLocationProvider implements IBookmarkLocationProvider {

	@Override
	public JavaTypeMemberBookmarkLocation getBookmarkLocation(Project project, Bookmark bookmark, ProgressIndicator progress) {
		return ReadAction.compute(() -> {
			List<PsiMember> memberCandidates = getMemberCandidates(project, bookmark, progress);
			for (PsiMember member : memberCandidates) {
				LinePosition linePosition = getLineNumber(member, bookmark, progress);
				if (linePosition != null) {
					return new JavaTypeMemberBookmarkLocation(member, linePosition.lineNumber, linePosition.lineOffset);
				}
			}
			if (memberCandidates.isEmpty()) {
				return null;
			} else {
				return new JavaTypeMemberBookmarkLocation(memberCandidates.get(0), null, null);
			}
		});
	}

	private LinePosition getLineNumber(PsiMember member, Bookmark bookmark, ProgressIndicator progress) {
		Integer estimatedLineNumber = getEstimatedLineNumber(member, bookmark);
		Integer lineNumber = estimatedLineNumber;
		Integer lineOffset;
		String lineContent = bookmark.getPropertyValue(TextEditorBookmarkProperties.PROP_LINE_CONTENT);

		PsiFile containingFile = member.getContainingFile();
		if (containingFile == null) {
			return null;
		}

		Document document = PsiDocumentManager.getInstance(member.getProject()).getDocument(containingFile);
		if (document == null) {
			return null;
		}

		if (lineContent != null) {
			DocumentFuzzySearcher searcher = new DocumentFuzzySearcher(document);
			TextRange region = getRegion(member);
			int foundLineNumber = searcher.findLineNumber(region,
					estimatedLineNumber == null ? -1 : estimatedLineNumber, lineContent, progress);
			lineNumber = foundLineNumber == -1 ? estimatedLineNumber : foundLineNumber;
		}
		if (lineNumber == null) {
			return null;
		}
		lineOffset = getLineOffset(document, lineNumber);
		if (lineOffset == null) {
			return null;
		}
		return new LinePosition(lineNumber, lineOffset);
	}

	private Integer getLineOffset(Document document, int lineNumber) {
		if (lineNumber < 0 || lineNumber >= document.getLineCount()) {
			return null;
		}
		return document.getLineStartOffset(lineNumber);
	}

	private TextRange getRegion(PsiMember member) {
		return member.getTextRange();
	}

	private Integer getEstimatedLineNumber(PsiMember member, Bookmark bookmark) {
		int lineNumber = JavaEditorUtils.getLineNumber(member);
		if (lineNumber == -1) {
			return null;
		}
		Integer lineNumberInsideElement = getLineNumberInsideElement(bookmark);
		if (lineNumberInsideElement != null) {
			lineNumber += lineNumberInsideElement;
		}
		return lineNumber;
	}

	private Integer getLineNumberInsideElement(Bookmark bookmark) {
		String lineNumberString = bookmark.getPropertyValue(PROP_LINE_NUMBER_INSIDE_ELEMENT);
		if (lineNumberString == null) {
			return null;
		}
		try {
			return Integer.parseInt(lineNumberString);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private List<PsiMember> getMemberCandidates(Project project, Bookmark javaBookmark, ProgressIndicator progress) {
		String type = javaBookmark.getPropertyValue(PROP_JAVA_TYPE);
		if (type != null) {
			List<PsiClass> matchingTypes = searchType(project, type);
			return new ArrayList<>(matchingTypes);
		}
		String declaringType = javaBookmark.getPropertyValue(PROP_JAVA_DECLARING_TYPE);
		if (declaringType == null) {
			return Collections.emptyList();
		}
		List<PsiMember> matchingMembers = searchType(project, declaringType).stream()
				.flatMap(matchingType -> getMemberCandidates(matchingType, javaBookmark).stream())
				.collect(Collectors.toList());
		return matchingMembers;
	}

	private List<PsiMember> getMemberCandidates(PsiClass type, Bookmark javaBookmark) {
		String elementKind = javaBookmark.getPropertyValue(PROP_JAVA_ELEMENT_KIND);
		String elementName = javaBookmark.getPropertyValue(PROP_JAVA_ELEMENT_NAME);
		if (KIND_FIELD.equals(elementKind)) {
			PsiField field = type.findFieldByName(elementName, false);
			return field != null ? List.of(field) : Collections.emptyList();
		}
		if (KIND_METHOD.equals(elementKind)) {
			String signature = javaBookmark.getPropertyValue(PROP_JAVA_METHOD_SIGNATURE);
			PsiMethod method = null;
			if (signature != null) {
				method = getMethod(type, elementName, signature);
			}
			if (method == null) {
				List<PsiMethod> candidates = getMethodsWithName(type, elementName);
				return new ArrayList<>(candidates);
			}
			return List.of(method);
		}
		if (isType(elementKind) && elementName != null) {
			PsiClass memberType = type.findInnerClassByName(elementName, false);
			return memberType != null ? List.of(memberType) : Collections.emptyList();
		}
		return Collections.emptyList();
	}

	private PsiMethod getMethod(PsiClass type, String name, String signature) {
		return getMethodsWithName(type, name).stream()
				.filter(method -> signature.equals(JavaEditorUtils.getMethodSimpleSignature(method))).findAny()
				.orElse(null);
	}

	private List<PsiMethod> getMethodsWithName(PsiClass type, String name) {
		List<PsiMethod> methodsWithName = new ArrayList<>();
		for (PsiMethod method : type.getMethods()) {
			if (name.equals(method.getName())) {
				methodsWithName.add(method);
			}
		}
		return methodsWithName;
	}

	private List<PsiClass> searchType(Project project, String classFQN) {
		classFQN = classFQN.replace('$', '.');
		List<PsiClass> types = new ArrayList<>();

		JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
		GlobalSearchScope scope = GlobalSearchScope.allScope(project);

		PsiClass psiClass = javaPsiFacade.findClass(classFQN, scope);
		if (psiClass != null) {
			types.add(psiClass);
		}

		return types;
	}

	private boolean isType(String elementKind) {
		return KIND_CLASS.equals(elementKind) || KIND_INTERFACE.equals(elementKind) ||
			   KIND_ENUM.equals(elementKind) || KIND_ANNOTATION.equals(elementKind) ||
			   KIND_TYPE.equals(elementKind);
	}

	private static class LinePosition {
		public final int lineNumber;
		public final int lineOffset;

		public LinePosition(int lineNumber, int lineOffset) {
			super();
			this.lineNumber = lineNumber;
			this.lineOffset = lineOffset;
		}

	}

}
