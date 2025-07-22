package mesfavoris.texteditor.internal;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.internal.placeholders.PathPlaceholderResolver;
import mesfavoris.internal.settings.placeholders.PathPlaceholdersStore;

import java.util.HashMap;
import java.util.Map;

import static mesfavoris.texteditor.TextEditorBookmarkProperties.*;
import static org.assertj.core.api.Assertions.assertThat;

public class TextEditorBookmarkPropertiesProviderTest extends BasePlatformTestCase {

	private TextEditorBookmarkPropertiesProvider textEditorBookmarkPropertiesProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// Copy test data to project
		myFixture.copyDirectoryToProject("commons-cli", "commons-cli");

		// Create the TextEditorBookmarkPropertiesProvider with required dependencies
		PathPlaceholdersStore placeholdersStore = new PathPlaceholdersStore();
		PathPlaceholderResolver pathPlaceholderResolver = new PathPlaceholderResolver(placeholdersStore);
		textEditorBookmarkPropertiesProvider = new TextEditorBookmarkPropertiesProvider(pathPlaceholderResolver);
	}

	@Override
	protected String getTestDataPath() {
		return "src/test/testData";
	}

	public void testBookmarkInsideTextFile() throws Exception {
		// Given
		PsiFile psiFile = myFixture.configureByFile("commons-cli/LICENSE.txt");
		Editor editor = myFixture.getEditor();
		VirtualFile virtualFile = psiFile.getVirtualFile();

		// Navigate to line 26 (0-based, so line 25) - this is the line with "Source" form text
		editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(25, 0));

		Map<String, String> bookmarkProperties = new HashMap<>();
		DataContext dataContext = SimpleDataContext.builder()
				.add(CommonDataKeys.EDITOR, editor)
				.add(CommonDataKeys.PROJECT, getProject())
				.add(CommonDataKeys.VIRTUAL_FILE, virtualFile)
				.build();

		// When
		textEditorBookmarkPropertiesProvider.addBookmarkProperties(bookmarkProperties, dataContext, new EmptyProgressIndicator());

		// Then
		assertThat(bookmarkProperties).containsEntry(PROP_LINE_NUMBER, "25")
				.containsEntry(PROP_PROJECT_NAME, getProject().getName())
				.containsEntry(PROP_LINE_CONTENT,
						"\"Source\" form shall mean the preferred form for making modifications,")
				.containsEntry(PROPERTY_NAME, "LICENSE.txt : \"Source\" form shall mean the preferred form for making modifications,")
				.containsEntry(PROP_WORKSPACE_PATH, "commons-cli/LICENSE.txt");
	}

}
