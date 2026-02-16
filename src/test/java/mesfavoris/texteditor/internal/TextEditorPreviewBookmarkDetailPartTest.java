package mesfavoris.texteditor.internal;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.UIUtil;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;
import mesfavoris.texteditor.TextEditorBookmarkProperties;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.Collections;

import static com.intellij.openapi.editor.EditorFactory.getInstance;
import static mesfavoris.java.JavaBookmarkProperties.PROP_JAVA_ELEMENT_NAME;
import static mesfavoris.path.PathBookmarkProperties.PROP_FILE_PATH;
import static mesfavoris.path.PathBookmarkProperties.PROP_WORKSPACE_PATH;
import static mesfavoris.tests.commons.waits.Waiter.waitUntil;

public class TextEditorPreviewBookmarkDetailPartTest extends BasePlatformTestCase {

    private TextEditorPreviewBookmarkDetailPart detailPart;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyDirectoryToProject("commons-cli", "commons-cli");
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    @Override
    public void tearDown() throws Exception {
        if (detailPart != null) {
            detailPart.dispose();
            detailPart = null;
        }
        super.tearDown();
    }

    @Test
    public void testCanHandle() {
        detailPart = new TextEditorPreviewBookmarkDetailPart(getProject());
        
        // Test with workspace file bookmark
        Bookmark workspaceBookmark = new Bookmark(new BookmarkId(), Collections.singletonMap(PROP_WORKSPACE_PATH, "/src/Main.java"));
        assertTrue(detailPart.canHandle(workspaceBookmark));

        // Test with file path bookmark
        Bookmark fileBookmark = new Bookmark(new BookmarkId(), Collections.singletonMap(PROP_FILE_PATH, "/tmp/test.txt"));
        assertTrue(detailPart.canHandle(fileBookmark));

        // Test with java element bookmark
        Bookmark javaBookmark = new Bookmark(new BookmarkId(), Collections.singletonMap(PROP_JAVA_ELEMENT_NAME, "MyClass"));
        assertTrue(detailPart.canHandle(javaBookmark));

        // Test with empty/other bookmark
        Bookmark otherBookmark = new Bookmark(new BookmarkId(), Collections.emptyMap());
        assertFalse(detailPart.canHandle(otherBookmark));
        
        Bookmark nullBookmark = null;
        assertFalse(detailPart.canHandle(nullBookmark));
    }

    @Test
    public void testPreviewUpdate() throws Exception {
        detailPart = new TextEditorPreviewBookmarkDetailPart(getProject());
        detailPart.createComponent();
        
        // Get the virtual file for CommandLine.java
        VirtualFile virtualFile = myFixture.findFileInTempDir("commons-cli/src/main/java/org/apache/commons/cli/CommandLine.java");
        assertNotNull("Test file not found", virtualFile);
        
        String workspacePath = "//" + getProject().getName() + "/commons-cli/src/main/java/org/apache/commons/cli/CommandLine.java";
        int targetLine = 50; // 0-based index

        Bookmark bookmark = new Bookmark(new BookmarkId(), new java.util.HashMap<>() {{
            put(PROP_WORKSPACE_PATH, workspacePath);
            put(TextEditorBookmarkProperties.PROP_LINE_NUMBER, String.valueOf(targetLine));
        }});

        // Set the bookmark
        detailPart.setBookmark(bookmark);

        // Wait for background tasks (ReadAction.nonBlocking) and UI updates
        Editor foundEditor = waitUntil("Editor for preview was not created", () -> {
            UIUtil.dispatchAllInvocationEvents();
            // Find the editor created for this file
            return findEditor(virtualFile);
        });
        
        assertNotNull("Editor for preview was not created", foundEditor);
        
        // Wait for scrolling (invokeLater)
        waitUntil("Caret should be at the target line", () -> {
            UIUtil.dispatchAllInvocationEvents();
            return foundEditor.getCaretModel().getLogicalPosition().line == targetLine;
        });
    }

    private @Nullable Editor findEditor(VirtualFile virtualFile) {
        Editor[] editors = getInstance().getAllEditors();
        for (Editor editor : editors) {
            if (editor.getProject() == getProject() &&
                FileDocumentManager.getInstance().getFile(editor.getDocument()).equals(virtualFile)) {
                return editor;
            }
        }
        return null;
    }
}
