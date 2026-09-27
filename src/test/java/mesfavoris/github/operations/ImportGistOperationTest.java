package mesfavoris.github.operations;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.github.GithubTestUser;
import mesfavoris.github.client.GistResponse;
import mesfavoris.github.client.IGistApiClient;
import mesfavoris.github.mappings.GistMapping;
import mesfavoris.github.mappings.GistMappingsStore;
import mesfavoris.github.test.GithubConnectionRule;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.modification.BookmarksTreeModifier;
import mesfavoris.persistence.json.BookmarksTreeJsonSerializer;
import mesfavoris.service.IBookmarksService;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static mesfavoris.github.mappings.GistMapping.BOOKMARKS_FILE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ImportGistOperationTest extends BasePlatformTestCase {

    private GithubConnectionRule connectionRule;
    private IGistApiClient apiClient;
    private GistMappingsStore mappings;
    private IBookmarksService bookmarksService;
    private final List<String> createdGistIds = new ArrayList<>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectionRule = new GithubConnectionRule(getProject(), GithubTestUser.USER1, true);
        connectionRule.before();
        apiClient = connectionRule.getConnectionManager().getGistApiClient();
        mappings = connectionRule.getGistMappingsStore();
        bookmarksService = getProject().getService(IBookmarksService.class);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            for (String gistId : createdGistIds) {
                apiClient.deleteGist(gistId);
            }
            connectionRule.after();
        } finally {
            super.tearDown();
        }
    }

    public void testImportGist_importsFolderFromCloneAndMapsIt() throws Exception {
        BookmarkId folderId = new BookmarkId();
        BookmarkId childId = new BookmarkId();
        BookmarksTreeModifier modifier = new BookmarksTreeModifier(
                new BookmarksTree(new BookmarkFolder(folderId, Map.of(Bookmark.PROPERTY_NAME, "imported"))));
        modifier.addBookmarks(folderId, List.of(new Bookmark(childId, Map.of(Bookmark.PROPERTY_NAME, "child"))));
        GistResponse gist = createGist(BOOKMARKS_FILE_NAME, serialize(modifier.getCurrentTree(), folderId));

        importGist(gist.id);

        BookmarksTree tree = bookmarksService.getBookmarksTree();
        assertThat(tree.getBookmark(folderId)).isInstanceOf(BookmarkFolder.class);
        assertThat(tree.getBookmark(childId)).isNotNull();
        GistMapping mapping = mappings.getMapping(folderId).orElseThrow();
        assertThat(mapping.getGistId()).isEqualTo(gist.id);
        assertThat(mapping.getProperties()).containsEntry(GistMapping.PROP_GIST_URL, gist.html_url);
        assertThat(connectionRule.getGistRepositories().getDirectory(gist.id).resolve(".git")).isDirectory();
    }

    public void testImportGist_withoutBookmarksFile_throwsIOException() throws Exception {
        GistResponse gist = createGist("other.json", "{}");

        assertThatThrownBy(() -> importGist(gist.id))
                .isInstanceOf(IOException.class)
                .hasMessageContaining(BOOKMARKS_FILE_NAME);
        assertThat(mappings.getMapping(gist.id)).isEmpty();
    }

    private void importGist(String gistId) throws Exception {
        new ImportGistOperation(apiClient, connectionRule.getConnectionManager(), mappings, bookmarksService)
                .importGist(bookmarksService.getBookmarksTree().getRootFolder().getId(), gistId, null);
    }

    private GistResponse createGist(String fileName, String content) throws IOException {
        GistResponse gist = apiClient.createGist("mesfavoris: import test", fileName, content);
        createdGistIds.add(gist.id);
        return gist;
    }

    private static String serialize(BookmarksTree tree, BookmarkId folderId) throws IOException {
        StringWriter writer = new StringWriter();
        new BookmarksTreeJsonSerializer(true).serialize(tree, folderId, writer);
        return writer.toString();
    }
}
