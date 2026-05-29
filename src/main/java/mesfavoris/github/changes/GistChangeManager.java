package mesfavoris.github.changes;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import mesfavoris.github.connection.GithubConnectionListener;
import mesfavoris.github.connection.GithubConnectionManager;
import mesfavoris.github.mappings.GistMapping;
import mesfavoris.github.mappings.IGistMappings;
import mesfavoris.github.operations.GistApiClient;
import mesfavoris.remote.IRemoteBookmarksStore.State;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Polls GitHub every {@link #DEFAULT_POLL_DELAY} seconds to detect remote changes using
 * conditional GET requests ({@code If-None-Match} / ETag). A 304 response means no change;
 * a 200 response means the Gist was modified and fires {@link IGistChangeListener}.
 * Polling starts on connect and stops on disconnect; ETags are cleared on disconnect so the
 * first poll after reconnect always fetches the current state.
 */
public class GistChangeManager implements Disposable {
    private static final Logger LOG = Logger.getInstance(GistChangeManager.class);
    public static final Duration DEFAULT_POLL_DELAY = Duration.ofSeconds(30);

    private final Project project;
    private final GithubConnectionManager connectionManager;
    private final IGistMappings gistMappings;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Supplier<Duration> pollDelayProvider;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Map<String, String> gistEtags = new ConcurrentHashMap<>();
    private final PollJob pollJob = new PollJob();

    private ScheduledFuture<?> scheduledFuture;

    public GistChangeManager(Project project, GithubConnectionManager connectionManager,
                              IGistMappings gistMappings, ScheduledExecutorService scheduledExecutorService) {
        this(project, connectionManager, gistMappings, scheduledExecutorService, () -> DEFAULT_POLL_DELAY);
    }

    public GistChangeManager(Project project, GithubConnectionManager connectionManager,
                              IGistMappings gistMappings, ScheduledExecutorService scheduledExecutorService,
                              Supplier<Duration> pollDelayProvider) {
        this.project = project;
        this.connectionManager = connectionManager;
        this.gistMappings = gistMappings;
        this.scheduledExecutorService = scheduledExecutorService;
        this.pollDelayProvider = pollDelayProvider;
    }

    public void init() {
        MessageBusConnection messageBusConnection = project.getMessageBus().connect(this);
        messageBusConnection.subscribe(GithubConnectionListener.TOPIC, new GithubConnectionListener() {
            @Override
            public void connected() {
                scheduleJob();
            }

            @Override
            public void disconnected() {
                cancelJob();
                gistEtags.clear();
            }
        });
        scheduleJob();
    }

    @Override
    public void dispose() {
        cancelJob();
        closed.set(true);
    }

    private synchronized void scheduleJob() {
        if (!shouldSchedule()) {
            return;
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduledFuture = scheduledExecutorService.schedule(pollJob, 0, TimeUnit.MILLISECONDS);
    }

    private synchronized void cancelJob() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
    }

    private boolean shouldSchedule() {
        return connectionManager.getState() == State.connected && !closed.get();
    }

    private void fireGistChanged(mesfavoris.model.BookmarkId bookmarkFolderId, String gistId) {
        project.getMessageBus().syncPublisher(IGistChangeListener.TOPIC)
                .gistChanged(bookmarkFolderId, gistId);
    }

    private class PollJob implements Runnable {
        @Override
        public void run() {
            try {
                if (connectionManager.getState() != State.connected) {
                    return;
                }
                String token = connectionManager.getAccessToken();
                if (token == null) {
                    return;
                }
                GistApiClient apiClient = new GistApiClient(() -> token, connectionManager::getApiBaseUrl,
                        java.net.http.HttpClient.newHttpClient(),
                        mesfavoris.github.GithubRemoteBookmarksStoreExtension.USER_AGENT);
                for (GistMapping mapping : gistMappings.getMappings()) {
                    String gistId = mapping.getGistId();
                    String storedEtag = gistEtags.get(gistId);
                    try {
                        String newEtag = apiClient.conditionalGetEtag(gistId, storedEtag);
                        if (newEtag != null) {
                            gistEtags.put(gistId, newEtag);
                            // Only fire if we had a previous ETag — first poll just seeds the cache
                            if (storedEtag != null) {
                                fireGistChanged(mapping.getBookmarkFolderId(), gistId);
                            }
                        }
                    } catch (IOException e) {
                        LOG.warn("Could not check gist " + gistId + " for changes", e);
                    }
                }
            } finally {
                synchronized (GistChangeManager.this) {
                    if (shouldSchedule()) {
                        scheduledFuture = scheduledExecutorService.schedule(
                                this, pollDelayProvider.get().toMillis(), TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }
}
