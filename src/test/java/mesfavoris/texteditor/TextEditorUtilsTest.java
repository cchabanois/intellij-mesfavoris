package mesfavoris.texteditor;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class TextEditorUtilsTest extends BasePlatformTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// Copy test data to project
		myFixture.copyDirectoryToProject("commons-cli", "commons-cli");
	}

	@Override
	protected String getTestDataPath() {
		return "src/test/testData";
	}

	public void testGetLineContent() throws Exception {
		// Given
		PsiFile psiFile = myFixture.configureByFile("commons-cli/LICENSE.txt");
		Editor editor = myFixture.getEditor();
		Document document = editor.getDocument();

		// When
		String lineContent = TextEditorUtils.getLineContent(document, 8);

		// Then
		assertThat(lineContent)
				.isEqualTo("      \"License\" shall mean the terms and conditions for use, reproduction,");
	}
}
