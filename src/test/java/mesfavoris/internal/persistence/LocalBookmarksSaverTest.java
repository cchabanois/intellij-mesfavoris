package mesfavoris.internal.persistence;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.json.BookmarksTreeJsonSerializer;
import mesfavoris.tests.commons.waits.Waiter;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for LocalBookmarksSaver
 */
public class LocalBookmarksSaverTest extends BasePlatformTestCase {

    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        return new TempDirTestFixtureImpl();
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    public void testSaveBookmarks() throws Exception {
        // Given
        BookmarkFolder rootFolder = new BookmarkFolder(new BookmarkId("root"), "Root");
        BookmarksTree originalTree = new BookmarksTree(rootFolder);
        Map<String, String> bookmarkProperties = new HashMap<>();
        bookmarkProperties.put(Bookmark.PROPERTY_NAME, "Test Bookmark");
        Bookmark bookmark = new Bookmark(new BookmarkId("test"), bookmarkProperties);
        originalTree = originalTree.addBookmarks(rootFolder.getId(), asList(bookmark));

        // Create test file using the temp dir fixture
        VirtualFile virtualFile = myFixture.getTempDirFixture().createFile("test-bookmarks.json");
        File file = new File(virtualFile.getPath());

        LocalBookmarksSaver saver = new LocalBookmarksSaver(file, new BookmarksTreeJsonSerializer(true));

        // When
        saver.saveBookmarks(originalTree);

        // Then
        assertThat(file).exists();
        assertThat(file.length()).isGreaterThan(0L);

        // Verify the content was written
        String content = Files.readString(file.toPath());
        assertThat(content).contains("Test Bookmark");
        assertThat(content).contains("root");
    }

    public void testSaveBookmarksRefreshesDocument() throws Exception {
        // Given
        BookmarkFolder rootFolder = new BookmarkFolder(new BookmarkId("root"), "Root");
        BookmarksTree originalTree = new BookmarksTree(rootFolder);
        Map<String, String> bookmarkProperties = new HashMap<>();
        bookmarkProperties.put(Bookmark.PROPERTY_NAME, "Test Bookmark");
        Bookmark bookmark = new Bookmark(new BookmarkId("test"), bookmarkProperties);
        originalTree = originalTree.addBookmarks(rootFolder.getId(), asList(bookmark));

        // Create test file and get its document
        VirtualFile virtualFile = myFixture.getTempDirFixture().createFile("test-bookmarks.json", "initial content");
        File file = new File(virtualFile.getPath());

        // Get the document and verify initial content
        Document document = ReadAction.compute(() ->
            FileDocumentManager.getInstance().getDocument(virtualFile));
        assertThat(document).isNotNull();
        assertThat(document.getText()).isEqualTo("initial content");

        LocalBookmarksSaver saver = new LocalBookmarksSaver(file, new BookmarksTreeJsonSerializer(true));

        // When
        saver.saveBookmarks(originalTree);

        // Then - wait for the document to be refreshed with new content
        Waiter.waitUntil("Document should be refreshed with new content", () -> {
            String documentContent = ReadAction.compute(document::getText);
            return documentContent.contains("Test Bookmark") &&
                   documentContent.contains("root") &&
                   !documentContent.contains("initial content");
        });
    }


}
