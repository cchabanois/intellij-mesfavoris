package mesfavoris.github.changes;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.MessageBusConnection;
import mesfavoris.github.GithubTestUser;
import mesfavoris.github.mappings.GistMappingsStore;
import mesfavoris.github.client.GistApiClient;
import mesfavoris.github.operations.LoadGistOperation;
import mesfavoris.github.test.GithubConnectionRule;
import mesfavoris.model.BookmarkId;
import mesfavoris.tests.commons.waits.Waiter;
import org.junit.Assume;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

public class GistChangeManagerTest extends BasePlatformTestCase {

    private GithubConnectionRule connectionRule;
    private GistChangeManager gistChangeManager;
    private GistMappingsStore gistMappingsStore;
    private MessageBusConnection messageBusConnection;
    private final GistChangeListener listener = new GistChangeListener();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Assume.assumeTrue("USER1_GITHUB_TOKEN not set", GithubTestUser.USER1.getToken().isPresent());
        connectionRule = new GithubConnectionRule(getProject(), GithubTestUser.USER1, true);
        connectionRule.before();
        gistMappingsStore = connectionRule.getGistMappingsStore();

        // Create two test gists and register their mappings
        String gistId1 = createGist("bookmarks for folder1");
        String gistId2 = createGist("bookmarks for folder2");
        gistMappingsStore.add(new BookmarkId("bookmarkFolder1"), gistId1, Collections.emptyMap());
        gistMappingsStore.add(new BookmarkId("bookmarkFolder2"), gistId2, Collections.emptyMap());

        ScheduledExecutorService executor = AppExecutorUtil.getAppScheduledExecutorService();
        gistChangeManager = new GistChangeManager(getProject(), connectionRule.getConnectionManager(),
                gistMappingsStore, executor, () -> Duration.ofSeconds(2));

        messageBusConnection = getProject().getMessageBus().connect();
        messageBusConnection.subscribe(IGistChangeListener.TOPIC, listener);

        gistChangeManager.init();

        // Wait until the initial poll has seeded ETags for both gists (proves GitHub has propagated them)
        String trackedGistId1 = gistMappingsStore.getMapping(new BookmarkId("bookmarkFolder1")).get().getGistId();
        String trackedGistId2 = gistMappingsStore.getMapping(new BookmarkId("bookmarkFolder2")).get().getGistId();
        Waiter.waitUntil("Initial ETags not seeded",
                () -> gistChangeManager.getGistEtags().containsKey(trackedGistId1)
                        && gistChangeManager.getGistEtags().containsKey(trackedGistId2),
                Duration.ofSeconds(30));
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (messageBusConnection != null) {
                messageBusConnection.disconnect();
            }
            if (gistChangeManager != null) {
                gistChangeManager.dispose();
            }
            if (connectionRule != null) {
                connectionRule.after();
            }
        } finally {
            super.tearDown();
        }
    }

    public void testListenerCalledWhenGistChanged() throws Exception {
        String gistId = gistMappingsStore.getMapping(new BookmarkId("bookmarkFolder1"))
                .get().getGistId();

        updateGist(gistId, "{\"updated\": true}");

        Waiter.waitUntil("Listener not called", () -> listener.getEvents().size() == 1,
                Duration.ofSeconds(10));
        Thread.sleep(3000); // wait > 1 poll cycle (2s) to confirm no duplicate events
        assertThat(listener.getEvents()).hasSize(1);
        assertThat(listener.getEvents().getFirst().bookmarkFolderId())
                .isEqualTo(new BookmarkId("bookmarkFolder1"));
    }

    public void testListenerNotCalledIfDisposed() throws Exception {
        String gistId = gistMappingsStore.getMapping(new BookmarkId("bookmarkFolder1"))
                .get().getGistId();
        gistChangeManager.dispose();

        updateGist(gistId, "{\"updated\": true}");

        Thread.sleep(3000); // wait > 1 poll cycle (2s) to confirm no events fired
        assertThat(listener.getEvents()).isEmpty();
    }

    private String createGist(String content) throws IOException {
        return apiClient().createGist("mesfavoris-test", LoadGistOperation.BOOKMARKS_FILE_NAME, content).id;
    }

    private void updateGist(String gistId, String content) throws Exception {
        apiClient().updateGist(gistId, LoadGistOperation.BOOKMARKS_FILE_NAME, content, null);
    }

    private GistApiClient apiClient() {
        String token = connectionRule.getConnectionManager().getAccessToken();
        String apiBaseUrl = connectionRule.getConnectionManager().getApiBaseUrl();
        return new GistApiClient(() -> token, () -> apiBaseUrl, java.net.http.HttpClient.newHttpClient());
    }

    private static class GistChangeListener implements IGistChangeListener {
        private final List<GistChangeEvent> events = new ArrayList<>();

        @Override
        public void gistChanged(BookmarkId bookmarkFolderId, String gistId) {
            events.add(new GistChangeEvent(bookmarkFolderId, gistId));
        }

        public List<GistChangeEvent> getEvents() {
            return events;
        }
    }

    private record GistChangeEvent(BookmarkId bookmarkFolderId, String gistId) {}
}
