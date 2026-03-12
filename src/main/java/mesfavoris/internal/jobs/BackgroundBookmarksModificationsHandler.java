package mesfavoris.internal.jobs;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import mesfavoris.BookmarksException;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.IBookmarksListener;
import mesfavoris.model.modification.BookmarksModification;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handle bookmarks modifications in a background job.
 * <p>
 * <p>
 * It does not delegate to the given {@link IBookmarksModificationsHandler}
 * until there is no more modifications during given delay.
 *
 * @author cchabanois
 */
public class BackgroundBookmarksModificationsHandler implements Disposable {
	private static final Logger LOG = Logger.getInstance(BackgroundBookmarksModificationsHandler.class);
	private final Project project;
	private final BookmarkDatabase bookmarkDatabase;
	private final IBookmarksModificationsHandler bookmarksModificationsHandler;
	private final Queue<BookmarksModification> eventsQueue = new ConcurrentLinkedQueue<BookmarksModification>();
	private final IBookmarksListener bookmarksListener;
	private final long scheduleDelay;
	private final AtomicInteger previousSize = new AtomicInteger(0);
	private final AtomicInteger unhandledEventsCount = new AtomicInteger(0);
	private final ScheduledExecutorService scheduledExecutorService;
	private final BookmarksModificationBatchHandlerJob job = new BookmarksModificationBatchHandlerJob();
	private ScheduledFuture scheduledFuture;

	public BackgroundBookmarksModificationsHandler(Project project, BookmarkDatabase bookmarkDatabase,
			IBookmarksModificationsHandler bookmarksModificationsHandler, long delay) {
		this.project = project;
		this.bookmarkDatabase = bookmarkDatabase;
		this.bookmarksModificationsHandler = bookmarksModificationsHandler;
		this.scheduleDelay = delay;
		this.scheduledExecutorService = AppExecutorUtil.getAppScheduledExecutorService();

		this.bookmarksListener = modifications -> {
			eventsQueue.addAll(modifications);
			unhandledEventsCount.set(eventsQueue.size());
			schedule();
		};
	}

	public void init() {
		bookmarkDatabase.addListener(bookmarksListener);
	}

	@Override
	public void dispose() {
		bookmarkDatabase.removeListener(bookmarksListener);
		synchronized (this) {
			if (scheduledFuture != null) {
				scheduledFuture.cancel(false);
			}
			List<BookmarksModification> modifications = getBookmarksModifications();
			if (!modifications.isEmpty()) {
				try {
					bookmarksModificationsHandler.handle(modifications, new EmptyProgressIndicator());
				} catch (BookmarksException e) {
					LOG.error("Cannot handle bookmarks modification while disposing", e);
				}
			}
		}
	}

	private synchronized void schedule() {
		if (scheduledFuture != null) {
			scheduledFuture.cancel(false);
		}
		scheduledFuture = scheduledExecutorService.schedule(job, scheduleDelay, TimeUnit.MILLISECONDS);
	}

	/**
	 * Number of modification events that have not yet been handled
	 * @return
	 */
	public int getQueueSize() {
		return unhandledEventsCount.get();
	}

	public List<BookmarksModification> getUnhandledEvents() {
		return new ArrayList<BookmarksModification>(eventsQueue);
	}

	private List<BookmarksModification> getBookmarksModifications() {
		List<BookmarksModification> list = new ArrayList<>();
		BookmarksModification event;
		while ((event = eventsQueue.poll()) != null) {
			list.add(event);
		}
		return list;
	}

	private class BookmarksModificationBatchHandlerJob implements Runnable {

		public void run() {
			synchronized (BackgroundBookmarksModificationsHandler.this) {
				if (previousSize.get() != eventsQueue.size()) {
					// don't handle modifications yet if there are new events
					previousSize.set(eventsQueue.size());
					schedule();
					return;
				}
				try {
					List<BookmarksModification> modifications = getBookmarksModifications();
					previousSize.set(0);
					if (modifications.isEmpty()) {
						return;
					}
					bookmarksModificationsHandler.handle(modifications, new EmptyProgressIndicator());
				} catch (BookmarksException e) {
					LOG.error("Cannot handle bookmarks modification", e);
					return;
				} finally {
					unhandledEventsCount.set(eventsQueue.size());
				}
			}

		}
	}

	public interface IBookmarksModificationsHandler {

		void handle(List<BookmarksModification> modifications, @NotNull ProgressIndicator progressIndicator)
				throws BookmarksException;

	}

}
