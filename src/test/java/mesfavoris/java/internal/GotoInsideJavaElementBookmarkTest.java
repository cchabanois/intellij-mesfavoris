package mesfavoris.java.internal;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;

import static mesfavoris.java.JavaBookmarkProperties.*;
import static org.assertj.core.api.Assertions.assertThat;

public class GotoInsideJavaElementBookmarkTest extends BasePlatformTestCase {

	private GotoInsideJavaElementBookmark gotoBookmark;
	private JavaTypeMemberBookmarkLocationProvider javaBookmarkLocationProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// Copy test data to project
		myFixture.copyDirectoryToProject("commons-cli", "commons-cli");

		gotoBookmark = new GotoInsideJavaElementBookmark();
		javaBookmarkLocationProvider = new JavaTypeMemberBookmarkLocationProvider();
	}

	@Override
	protected String getTestDataPath() {
		return "src/test/testData";
	}

	public void testGotoInsideMethod() {
		// Given
		Bookmark bookmark = new Bookmark(new BookmarkId(),
				ImmutableMap.of(PROP_JAVA_DECLARING_TYPE, "org.apache.commons.cli.DefaultParser",
						PROP_JAVA_ELEMENT_KIND, KIND_METHOD, PROP_JAVA_ELEMENT_NAME, "handleProperties",
						PROP_LINE_NUMBER_INSIDE_ELEMENT, "7"));
		JavaTypeMemberBookmarkLocation location = javaBookmarkLocationProvider.getBookmarkLocation(
				getProject(), bookmark, new EmptyProgressIndicator());

		// When
		boolean result = gotoBookmark.gotoBookmark(getProject(), bookmark, location);

		// Then
		assertThat(result).isTrue();

		// Verify that the correct file is opened and cursor is at the right position
		Editor editor = FileEditorManager.getInstance(getProject()).getSelectedTextEditor();
		assertThat(editor).isNotNull();

		// Verify the file name
		String fileName = editor.getVirtualFile().getName();
		assertThat(fileName).isEqualTo("DefaultParser.java");

		// Verify cursor position - should be at line 146 (7 lines inside handleProperties method)
		// The handleProperties method starts at line 140, so line 140 + 7 = 147 (but 0-based indexing makes it 146)
		LogicalPosition cursorPosition = editor.getCaretModel().getLogicalPosition();
		assertThat(cursorPosition.line).isEqualTo(146);
	}

}
