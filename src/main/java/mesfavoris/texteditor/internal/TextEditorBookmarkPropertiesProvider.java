package mesfavoris.texteditor.internal;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import mesfavoris.bookmarktype.AbstractBookmarkPropertiesProvider;
import mesfavoris.placeholders.IPathPlaceholderResolver;
import mesfavoris.texteditor.TextEditorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;

import static mesfavoris.texteditor.TextEditorBookmarkProperties.*;

public class TextEditorBookmarkPropertiesProvider extends AbstractBookmarkPropertiesProvider {
	private final IPathPlaceholderResolver pathPlaceholderResolver;

	public TextEditorBookmarkPropertiesProvider(IPathPlaceholderResolver pathPlaceholders) {
		this.pathPlaceholderResolver = pathPlaceholders;
	}

	@Override
	public void addBookmarkProperties(Map<String, String> bookmarkProperties, DataContext dataContext, ProgressIndicator progress) {
		// readAction needed for VirtualFile
		ReadAction.run(() -> {
			Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
			VirtualFile virtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE);
			Project project = dataContext.getData(CommonDataKeys.PROJECT);
			if (editor == null || virtualFile == null) {
				return;
			}
			addBookmarkProperties(bookmarkProperties, editor, virtualFile);
		});
	}

	private void addBookmarkProperties(Map<String, String> properties, Editor editor, VirtualFile virtualFile) {
		SelectionModel selectionModel = editor.getSelectionModel();
		LogicalPosition selectionStart = editor.offsetToLogicalPosition(selectionModel.getSelectionStart());
		int lineNumber = selectionStart.line;
		addLineNumber(properties, lineNumber);
		addLineContent(properties, editor, lineNumber);
		addWorkspacePath(properties, editor.getProject(), virtualFile);
		putIfAbsent(properties, PROPERTY_NAME, () -> {
			String lineContent = properties.get(PROP_LINE_CONTENT);
			if (lineContent != null) {
				return virtualFile.getPresentableName() + " : " + lineContent;
			} else {
				return virtualFile.getPresentableName();
			}
		});
		addFilePath(properties, virtualFile);
	}

	private void addLineContent(Map<String, String> properties, Editor editor, int lineNumber) {
		putIfAbsent(properties, PROP_LINE_CONTENT, () -> {
			String content = TextEditorUtils.getLineContent(editor.getDocument(), lineNumber);
			return content == null ? null : content.trim();
		});
	}

	private void addLineNumber(Map<String, String> properties, int lineNumber) {
		putIfAbsent(properties, PROP_LINE_NUMBER, Integer.toString(lineNumber));
	}

	private void addWorkspacePath(Map<String, String> properties, Project project, VirtualFile virtualFile) {
		if (project == null) {
			return;
		}
		putIfAbsent(properties, PROP_WORKSPACE_PATH, getRelativePath(project, virtualFile));
		putIfAbsent(properties, PROP_PROJECT_NAME, project.getName());
	}

	private void addFilePath(Map<String, String> properties, VirtualFile virtualFile) {

		putIfAbsent(properties, PROP_FILE_PATH, () -> {
			try {
				Path path = virtualFile.toNioPath();
				return pathPlaceholderResolver.collapse(path);
			} catch (UnsupportedOperationException e) {
				return null;
			}
		});
	}

	@Nullable
	private String getRelativePath( @NotNull Project project,  @NotNull VirtualFile virtualFile) {
		ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
		VirtualFile contentRoot = projectFileIndex.getContentRootForFile(virtualFile);

		if (contentRoot != null) {
			return virtualFile.getPath().substring(contentRoot.getPath().length() + 1);
		}

		return null;
	}

}
