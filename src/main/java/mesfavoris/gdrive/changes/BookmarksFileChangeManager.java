package mesfavoris.gdrive.changes;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Change;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.util.containers.ContainerUtil;
import mesfavoris.gdrive.connection.GDriveConnectionManager;
import mesfavoris.gdrive.connection.IConnectionListener;
import mesfavoris.gdrive.mappings.BookmarkMapping;
import mesfavoris.gdrive.mappings.IBookmarkMappings;
import mesfavoris.gdrive.operations.GetChangesOperation;
import mesfavoris.model.BookmarkId;
import mesfavoris.remote.IRemoteBookmarksStore.State;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Listen to changes to bookmark files
 *
 * @author cchabanois
 *
 */
public class BookmarksFileChangeManager {
	private static final Logger LOG = Logger.getInstance(BookmarksFileChangeManager.class);
	public static final Duration DEFAULT_POLL_DELAY = Duration.ofSeconds(30);

	private final GDriveConnectionManager gdriveConnectionManager;
	private final IConnectionListener connectionListener = new ConnectionListener();
	private final BookmarksFileChangeJob job = new BookmarksFileChangeJob();
	private final Supplier<Duration> pollDelayProvider;
	private final IBookmarkMappings bookmarkMappings;
	private final List<IBookmarksFileChangeListener> listenerList = ContainerUtil.createLockFreeCopyOnWriteList();
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private final ScheduledExecutorService scheduledExecutorService;
	private ScheduledFuture<?> scheduledFuture;
	
	public BookmarksFileChangeManager(GDriveConnectionManager gdriveConnectionManager,
			IBookmarkMappings bookmarkMappings, ScheduledExecutorService scheduledExecutorService) {
		this(gdriveConnectionManager, bookmarkMappings, scheduledExecutorService, () -> DEFAULT_POLL_DELAY);
	}

	public BookmarksFileChangeManager(GDriveConnectionManager gdriveConnectionManager,
			IBookmarkMappings bookmarkMappings, ScheduledExecutorService scheduledExecutorService,
			Supplier<Duration> pollDelayProvider) {
		this.gdriveConnectionManager = gdriveConnectionManager;
		this.bookmarkMappings = bookmarkMappings;
		this.scheduledExecutorService = scheduledExecutorService;
		this.pollDelayProvider = pollDelayProvider;
	}

	public void init() {
		gdriveConnectionManager.addConnectionListener(connectionListener);
		// won't be scheduled if not connected
		scheduleJob();
	}

	public void close() {
		gdriveConnectionManager.removeConnectionListener(connectionListener);
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
		scheduledFuture = scheduledExecutorService.schedule(job, 0, TimeUnit.MILLISECONDS);
	}

	private synchronized void cancelJob() {
		if (scheduledFuture != null) {
			scheduledFuture.cancel(false);
			scheduledFuture = null;
		}
	}

	private boolean shouldSchedule() {
		return gdriveConnectionManager.getState() == State.connected && !closed.get();
	}

	public void addListener(IBookmarksFileChangeListener listener) {
		listenerList.add(listener);
	}

	public void removeListener(IBookmarksFileChangeListener listener) {
		listenerList.remove(listener);
	}

	private void fireBookmarksFileChanged(BookmarkId bookmarkFolderId, Change change) {
		for (IBookmarksFileChangeListener listener : listenerList) {
			try {
				listener.bookmarksFileChanged(bookmarkFolderId, change);
			} catch (Exception e) {
				LOG.error("Error in bookmarks file change listener", e);
			}
		}
	}

	private class ConnectionListener implements IConnectionListener {

		@Override
		public void connected() {
			scheduleJob();
		}

		@Override
		public void disconnected() {
			cancelJob();
		}

	}

	private class BookmarksFileChangeJob implements Runnable {
		private Long startChangeId;

		@Override
		public void run() {
			try {
				Drive drive = gdriveConnectionManager.getDrive();
				if (drive == null) {
					return;
				}
				GetChangesOperation operation = new GetChangesOperation(drive);
				if (startChangeId == null) {
					startChangeId = operation.getLargestChangeId() + 1;
				}
				List<Change> changes = operation.getChanges(startChangeId);
				for (Change change : changes) {
					Optional<BookmarkMapping> bookmarkMapping = bookmarkMappings.getMapping(change.getFileId());
                    bookmarkMapping.ifPresent(mapping -> fireBookmarksFileChanged(mapping.getBookmarkFolderId(), change));
				}
				if (!changes.isEmpty()) {
					startChangeId = changes.get(changes.size() - 1).getId() + 1;
				}
			} catch (UnknownHostException | SocketTimeoutException e) {
				gdriveConnectionManager.disconnect(new EmptyProgressIndicator());
			} catch (IOException e) {
				LOG.warn("Could not get remote bookmark changes", e);
			} finally {
				synchronized (BookmarksFileChangeManager.this) {
					if (shouldSchedule()) {
						scheduledFuture = scheduledExecutorService.schedule(this, pollDelayProvider.get().toMillis(), TimeUnit.MILLISECONDS);
					}
				}
			}
		}

	}

}
