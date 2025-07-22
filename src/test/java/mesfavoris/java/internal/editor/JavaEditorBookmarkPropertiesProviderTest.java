package mesfavoris.java.internal.editor;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.HashMap;
import java.util.Map;

import static mesfavoris.java.JavaBookmarkProperties.*;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_LINE_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaEditorBookmarkPropertiesProviderTest extends BasePlatformTestCase {

	private JavaEditorBookmarkPropertiesProvider javaEditorBookmarkPropertiesProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// Copy test data to project
		myFixture.copyDirectoryToProject("commons-cli", "commons-cli");

		javaEditorBookmarkPropertiesProvider = new JavaEditorBookmarkPropertiesProvider();
	}

	@Override
	protected String getTestDataPath() {
		return "src/test/testData";
	}

	public void testBookmarkInsideMethod() throws Exception {
		// Given
		PsiFile psiFile = myFixture.configureByFile("commons-cli/src/main/java/org/apache/commons/cli/BasicParser.java");
		Editor editor = myFixture.getEditor();
		VirtualFile virtualFile = psiFile.getVirtualFile();

		// Navigate to line 48 (0-based, so line 49) - this is "// just echo the arguments" inside flatten method
		editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(48, 0));

		Map<String, String> bookmarkProperties = new HashMap<>();
		DataContext dataContext = SimpleDataContext.builder()
				.add(CommonDataKeys.EDITOR, editor)
				.add(CommonDataKeys.PROJECT, getProject())
				.add(CommonDataKeys.VIRTUAL_FILE, virtualFile)
				.build();

		// When
		javaEditorBookmarkPropertiesProvider.addBookmarkProperties(bookmarkProperties, dataContext, new EmptyProgressIndicator());

		// Then
		assertThat(bookmarkProperties).containsEntry(PROP_LINE_NUMBER_INSIDE_ELEMENT, "5")
				.containsEntry(PROP_JAVA_DECLARING_TYPE, "org.apache.commons.cli.BasicParser")
				.containsEntry(PROP_JAVA_ELEMENT_NAME, "flatten").containsEntry(PROP_JAVA_ELEMENT_KIND, KIND_METHOD)
				.containsEntry(PROP_LINE_CONTENT, "return arguments;")
				.containsEntry(PROP_JAVA_METHOD_SIGNATURE, "String[] flatten(Options,String[],boolean)");
	}

	public void testBookmarkBeforeDeclarationButOnSameLine() throws Exception {
		// Given
		PsiFile psiFile = myFixture.configureByFile("commons-cli/src/main/java/org/apache/commons/cli/Parser.java");
		Editor editor = myFixture.getEditor();
		VirtualFile virtualFile = psiFile.getVirtualFile();

		// Navigate to line 45 (0-based, so line 44) - this is "protected void setOptions(Options options)" method declaration
		editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(44, 0));

		Map<String, String> bookmarkProperties = new HashMap<>();
		DataContext dataContext = SimpleDataContext.builder()
				.add(CommonDataKeys.EDITOR, editor)
				.add(CommonDataKeys.PROJECT, getProject())
				.add(CommonDataKeys.VIRTUAL_FILE, virtualFile)
				.build();

		// When
		javaEditorBookmarkPropertiesProvider.addBookmarkProperties(bookmarkProperties, dataContext, new EmptyProgressIndicator());

		// Then
		assertThat(bookmarkProperties).containsEntry(PROP_LINE_NUMBER_INSIDE_ELEMENT, "0")
				.containsEntry(PROP_JAVA_DECLARING_TYPE, "org.apache.commons.cli.Parser")
				.containsEntry(PROP_JAVA_ELEMENT_NAME, "setOptions").containsEntry(PROP_JAVA_ELEMENT_KIND, KIND_METHOD)
				.containsEntry(PROP_LINE_CONTENT, "protected void setOptions(Options options)")
				.containsEntry(PROP_JAVA_METHOD_SIGNATURE, "void setOptions(Options)");
	}
}
