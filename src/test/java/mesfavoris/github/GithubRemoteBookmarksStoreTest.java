package mesfavoris.github;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.concurrency.AppExecutorUtil;
import mesfavoris.github.changes.GistChangeManager;
import mesfavoris.github.test.GithubConnectionRule;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.remote.ConflictException;
import mesfavoris.remote.IRemoteBookmarksStore.State;
import mesfavoris.remote.RemoteBookmarksStoreDescriptor;
import mesfavoris.remote.RemoteBookmarksTree;
import org.junit.Assume;

import javax.swing.*;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GithubRemoteBookmarksStoreTest extends BasePlatformTestCase {

    private static final String ID = "github";
    private GithubRemoteBookmarksStore store;
    private GithubConnectionRule connectionRule;
    private RemoteBookmarksStoreDescriptor descriptor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Assume.assumeTrue("USER1_GITHUB_TOKEN not set", GithubTestUser.USER1.getToken().isPresent());
        ScheduledExecutorService executor = AppExecutorUtil.getAppScheduledExecutorService();
        connectionRule = new GithubConnectionRule(getProject(), GithubTestUser.USER1, false);
        connectionRule.before();
        GistChangeManager changeManager = new GistChangeManager(getProject(),
                connectionRule.getConnectionManager(), connectionRule.getGistMappingsStore(),
                executor, () -> Duration.ofSeconds(30));
        store = new GithubRemoteBookmarksStore(getProject(), connectionRule.getConnectionManager(),
                connectionRule.getGistMappingsStore(), changeManager);
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

    public void testConflictWhenSaving() throws Exception {
        BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(new BookmarkId("tree1"), Maps.newHashMap()));
        connect();
        store.add(bookmarksTree, bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());
        // Load to get the current etag (updated_at may differ from what add() returned)
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

    private void connect() throws IOException {
        store.connect(new EmptyProgressIndicator());
    }
}
