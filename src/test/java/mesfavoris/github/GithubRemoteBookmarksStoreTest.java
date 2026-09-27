package mesfavoris.github;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.concurrency.AppExecutorUtil;
import mesfavoris.github.changes.GistChangeManager;
import mesfavoris.github.mappings.GistMapping;
import mesfavoris.github.mappings.GistMappingsStore;
import mesfavoris.github.repository.GistRepositories;
import mesfavoris.github.test.GithubConnectionRule;
import mesfavoris.internal.persistence.RemoteBookmarksSaver;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.modification.BookmarksTreeModifier;
import mesfavoris.persistence.json.BookmarksTreeJsonSerializer;
import mesfavoris.remote.ConflictException;
import mesfavoris.remote.IRemoteBookmarksStore.State;
import mesfavoris.remote.RemoteBookmarksStoreDescriptor;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.remote.RemoteBookmarksTree;
import mesfavoris.tests.commons.waits.Waiter;

import javax.swing.*;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GithubRemoteBookmarksStoreTest extends BasePlatformTestCase {

    private static final String ID = "github";
    private GithubRemoteBookmarksStore store;
    private GithubConnectionRule connectionRule;
    private RemoteBookmarksStoreDescriptor descriptor;
    /** Run once before the next save, to simulate another client changing the gist meanwhile. */
    private RemoteChange beforeNextSave;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ScheduledExecutorService executor = AppExecutorUtil.getAppScheduledExecutorService();
        connectionRule = new GithubConnectionRule(getProject(), GithubTestUser.USER1, false);
        connectionRule.before();
        GistChangeManager changeManager = new GistChangeManager(getProject(),
                connectionRule.getConnectionManager(), connectionRule.getGistMappingsStore(),
                executor, () -> Duration.ofSeconds(30));
        store = new GithubRemoteBookmarksStore(getProject(), connectionRule.getConnectionManager(),
                connectionRule.getGistMappingsStore(), changeManager) {
            @Override
            public RemoteBookmarksTree save(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId, String etag,
                                            ProgressIndicator indicator) throws IOException, ConflictException {
                RemoteChange change = beforeNextSave;
                beforeNextSave = null;
                if (change != null) {
                    try {
                        change.apply();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                return super.save(bookmarksTree, bookmarkFolderId, etag, indicator);
            }
        };
        descriptor = new RemoteBookmarksStoreDescriptor(ID, "GitHub Gists", new ImageIcon(), new ImageIcon());
        store.init(descriptor);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (store != null && store.getState() == State.connected) {
                store.disconnect(new EmptyProgressIndicator());
            }
            if (connectionRule != null) {
                connectionRule.after();
            }
        } finally {
            super.tearDown();
        }
    }

    public void testConnect() throws IOException {
        connect();

        assertThat(store.getState()).isEqualTo(State.connected);
    }

    public void testDisconnect() throws IOException {
        connect();

        store.disconnect(new EmptyProgressIndicator());

        assertThat(store.getState()).isEqualTo(State.disconnected);
    }

    public void testAdd() throws IOException {
        BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(new BookmarkId("tree1"), Maps.newHashMap()));
        connect();

        store.add(bookmarksTree, bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());

        RemoteBookmarksTree remote = store.load(bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());
        assertThat(bookmarksTree.toString()).isEqualTo(remote.getBookmarksTree().toString());
        assertThat(store.getRemoteBookmarkFolder(bookmarksTree.getRootFolder().getId())).isPresent();
    }

    public void testRemove() throws IOException {
        BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(new BookmarkId("tree1"), Maps.newHashMap()));
        connect();
        store.add(bookmarksTree, bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());

        store.remove(bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());

        assertThat(store.getRemoteBookmarkFolders()).isEqualTo(Sets.newHashSet());
    }

    public void testSave() throws Exception {
        BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(new BookmarkId("tree1"), Maps.newHashMap()));
        connect();
        store.add(bookmarksTree, bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());

        bookmarksTree = bookmarksTree.setPropertyValue(bookmarksTree.getRootFolder().getId(),
                "myProperty", "myPropertyValue");
        store.save(bookmarksTree, bookmarksTree.getRootFolder().getId(), null, new EmptyProgressIndicator());

        RemoteBookmarksTree remote = store.load(bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());
        assertThat(bookmarksTree.toString()).isEqualTo(remote.getBookmarksTree().toString());
    }

    public void testSave_withoutEtag_savesOnTopOfLatestVersion() throws Exception {
        BookmarkId folderId = new BookmarkId("tree1");
        BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(folderId, Maps.newHashMap()));
        connect();
        store.add(bookmarksTree, folderId, new EmptyProgressIndicator());
        store.load(folderId, new EmptyProgressIndicator());
        String gistId = connectionRule.getGistMappingsStore().getMapping(folderId).orElseThrow().getGistId();
        // another client changes the gist after our last load
        connectionRule.getConnectionManager().getGistApiClient().updateGist(gistId, GistMapping.BOOKMARKS_FILE_NAME,
                serialize(addBookmark(bookmarksTree, folderId, "b2"), folderId), null);
        BookmarksTree savedTree = bookmarksTree.setPropertyValue(folderId, "myProperty", "myPropertyValue");

        store.save(savedTree, folderId, null, new EmptyProgressIndicator());

        RemoteBookmarksTree remote = store.load(folderId, new EmptyProgressIndicator());
        assertThat(remote.getBookmarksTree().toString()).isEqualTo(savedTree.toString());
    }

    public void testConflictWhenSaving() throws Exception {
        BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(new BookmarkId("tree1"), Maps.newHashMap()));
        connect();
        store.add(bookmarksTree, bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());
        RemoteBookmarksTree remote = store.load(bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());

        BookmarksTree tree2 = bookmarksTree.setPropertyValue(bookmarksTree.getRootFolder().getId(),
                "myProperty", "value1");
        store.save(tree2, tree2.getRootFolder().getId(), remote.getEtag(), new EmptyProgressIndicator());

        BookmarksTree tree3 = bookmarksTree.setPropertyValue(bookmarksTree.getRootFolder().getId(),
                "myProperty", "value2");
        assertThatThrownBy(() -> store.save(tree3, tree3.getRootFolder().getId(),
                remote.getEtag(), new EmptyProgressIndicator()))
                .isInstanceOf(ConflictException.class);
    }

    public void testAdd_returnsCommitIdOfCreatedGistAsEtag() throws Exception {
        BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(new BookmarkId("tree1"), Maps.newHashMap()));
        connect();

        RemoteBookmarksTree added = store.add(bookmarksTree, bookmarksTree.getRootFolder().getId(),
                new EmptyProgressIndicator());

        RemoteBookmarksTree loaded = store.load(bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());
        assertThat(added.getEtag()).isNotNull().isEqualTo(loaded.getEtag());
    }

    public void testConcurrentRemoteChange_localChangeIsReplayedOnTopOfIt() throws Exception {
        BookmarkId folderId = new BookmarkId("tree1");
        BookmarksTree original = addBookmark(new BookmarksTree(new BookmarkFolder(folderId, Maps.newHashMap())),
                folderId, "b1");
        connect();
        store.add(original, folderId, new EmptyProgressIndicator());
        String gistId = connectionRule.getGistMappingsStore().getMapping(folderId).orElseThrow().getGistId();
        // another client adds b2 between the saver's load and its save
        String remoteContent = serialize(addBookmark(original, folderId, "b2"), folderId);
        beforeNextSave = () -> connectionRule.getConnectionManager().getGistApiClient()
                .updateGist(gistId, GistMapping.BOOKMARKS_FILE_NAME, remoteContent, null);
        BookmarksTreeModifier localChange = new BookmarksTreeModifier(original);
        localChange.addBookmarks(folderId, List.of(new Bookmark(new BookmarkId("b3"))));
        RemoteBookmarksSaver saver = new RemoteBookmarksSaver(new RemoteBookmarksStoreManager(() -> List.of(store)));

        saver.applyModificationsToRemoteBookmarksStores(localChange.getModifications(), new EmptyProgressIndicator());

        BookmarksTree saved = store.load(folderId, new EmptyProgressIndicator()).getBookmarksTree();
        assertThat(saved.getChildren(folderId)).extracting(Bookmark::getId)
                .containsExactlyInAnyOrder(new BookmarkId("b1"), new BookmarkId("b2"), new BookmarkId("b3"));
    }

    public void testLoad_ignoresGitUrlStoredInMapping() throws Exception {
        BookmarkId folderId = new BookmarkId("tree1");
        BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(folderId, Maps.newHashMap()));
        connect();
        store.add(bookmarksTree, folderId, new EmptyProgressIndicator());
        // a tampered project file must not redirect git (and the token) to another server
        GistMappingsStore mappings = connectionRule.getGistMappingsStore();
        GistMapping mapping = mappings.getMapping(folderId).orElseThrow();
        Map<String, String> tamperedProperties = new HashMap<>(mapping.getProperties());
        tamperedProperties.put("gitUrl", "http://127.0.0.1:9/evil.git");
        mappings.update(mapping.getGistId(), tamperedProperties);

        RemoteBookmarksTree remote = store.load(folderId, new EmptyProgressIndicator());

        assertThat(remote.getBookmarksTree().toString()).isEqualTo(bookmarksTree.toString());
    }

    public void testLoad_mappingWithInvalidGistId_throwsIOException() throws Exception {
        BookmarkId folderId = new BookmarkId("tree1");
        connect();
        // a tampered project file must not make a gist id point outside the local clones
        connectionRule.getGistMappingsStore().add(folderId, "../../outside", Map.of());

        assertThatThrownBy(() -> store.load(folderId, new EmptyProgressIndicator()))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid gist id");
    }

    public void testRemove_deletesLocalClone() throws Exception {
        BookmarkId folderId = new BookmarkId("tree1");
        Path clone = addAndClone(folderId);

        store.remove(folderId, new EmptyProgressIndicator());

        Waiter.waitUntil("Local clone not deleted", () -> !Files.exists(clone), Duration.ofSeconds(10));
    }

    public void testMappingRemoved_deletesLocalClone() throws Exception {
        BookmarkId folderId = new BookmarkId("tree1");
        Path clone = addAndClone(folderId);

        // what happens when the bookmark folder is deleted locally
        connectionRule.getGistMappingsStore().remove(folderId);

        Waiter.waitUntil("Local clone not deleted", () -> !Files.exists(clone), Duration.ofSeconds(10));
    }

    private Path addAndClone(BookmarkId folderId) throws IOException {
        BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(folderId, Maps.newHashMap()));
        connect();
        store.add(bookmarksTree, folderId, new EmptyProgressIndicator());
        store.load(folderId, new EmptyProgressIndicator());
        String gistId = connectionRule.getGistMappingsStore().getMapping(folderId).orElseThrow().getGistId();
        Path clone = GistRepositories.getInstance().getDirectory(gistId);
        assertThat(clone.resolve(".git")).isDirectory();
        return clone;
    }

    private static BookmarksTree addBookmark(BookmarksTree tree, BookmarkId folderId, String bookmarkId) {
        BookmarksTreeModifier modifier = new BookmarksTreeModifier(tree);
        modifier.addBookmarks(folderId, List.of(new Bookmark(new BookmarkId(bookmarkId))));
        return modifier.getCurrentTree();
    }

    private static String serialize(BookmarksTree tree, BookmarkId folderId) throws IOException {
        StringWriter writer = new StringWriter();
        new BookmarksTreeJsonSerializer(true).serialize(tree, folderId, writer);
        return writer.toString();
    }

    private void connect() throws IOException {
        store.connect(new EmptyProgressIndicator());
    }

    private interface RemoteChange {
        void apply() throws Exception;
    }
}
