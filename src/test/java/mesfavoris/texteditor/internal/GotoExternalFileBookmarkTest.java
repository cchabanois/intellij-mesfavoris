package mesfavoris.texteditor.internal;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class GotoExternalFileBookmarkTest extends BasePlatformTestCase {

    private GotoExternalFileBookmark gotoExternalFileBookmark;
    private File tempDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        gotoExternalFileBookmark = new GotoExternalFileBookmark();
        tempDir = FileUtil.createTempDirectory("intellij-mesfavoris-test", "");
    }

    @Override
    protected void tearDown() throws Exception {
        // Clean up temporary files
        if (tempDir != null && tempDir.exists()) {
            FileUtil.deleteRecursively(tempDir.toPath());
        }
        super.tearDown();
    }

    private File fileWithContent(String name, String content) throws Exception {
        File file = new File(tempDir, name);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }

    private void assertFileIsOpenInEditor(VirtualFile testFile) {
        VirtualFile[] openFiles = FileEditorManager.getInstance(getProject()).getOpenFiles();
        boolean fileIsOpen = Arrays.asList(openFiles).contains(testFile);
        assertTrue("File should be open in editor", fileIsOpen);
    }

    public void testGotoBookmarkWithValidPath() throws Exception {
        // Given
        File file = fileWithContent("test.txt", "Line 1\nLine 2\nLine 3\nLine 4\nLine 5");
        Path filePath = file.toPath();
        ExternalFileBookmarkLocation bookmarkLocation = new ExternalFileBookmarkLocation(filePath, null, null);
        Bookmark bookmark = new Bookmark(new BookmarkId());

        // When
        boolean result = gotoExternalFileBookmark.gotoBookmark(getProject(), bookmark, bookmarkLocation);

        // Then
        assertThat(result).isTrue();
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        assertFileIsOpenInEditor(virtualFile);
    }

    public void testGotoBookmarkWithValidPathAndLineNumber() throws Exception {
        // Given
        File file = fileWithContent("test.txt", "Line 1\nLine 2\nLine 3\nLine 4\nLine 5");
        Path filePath = file.toPath();
        int lineNumber = 2;
        ExternalFileBookmarkLocation bookmarkLocation = new ExternalFileBookmarkLocation(filePath, lineNumber, null);
        Bookmark bookmark = new Bookmark(new BookmarkId());

        // When
        boolean result = gotoExternalFileBookmark.gotoBookmark(getProject(), bookmark, bookmarkLocation);

        // Then
        assertThat(result).isTrue();
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        assertFileIsOpenInEditor(virtualFile);
        Editor editor = FileEditorManager.getInstance(getProject()).getSelectedTextEditor();
        assertThat(editor.getCaretModel().getLogicalPosition().line).isEqualTo(lineNumber);
    }

    public void testGotoBookmarkWithNonExistentFile() {
        // Given
        File nonExistentFile = new File(tempDir, "nonexistent.txt");
        Path filePath = nonExistentFile.toPath();
        ExternalFileBookmarkLocation bookmarkLocation = new ExternalFileBookmarkLocation(filePath, null, null);
        Bookmark bookmark = new Bookmark(new BookmarkId());

        // When
        boolean result = gotoExternalFileBookmark.gotoBookmark(getProject(), bookmark, bookmarkLocation);

        // Then
        assertFalse(result);
    }

}
