package mesfavoris.internal.jobs;

import com.intellij.util.concurrency.AppExecutorUtil;
import mesfavoris.BookmarksException;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.IBookmarksListener;
import mesfavoris.model.modification.BookmarksModification;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handle bookmarks modifications in a background job.
 * 
 * 
 * It does not delegate to the given {@link IBookmarksModificationsHandler}
 * until there is no more modifications during given delay.
 * 
 * @author cchabanois
 *
 */
public class BackgroundBookmarksModificationsHandler {
	private final BookmarkDatabase bookmarkDatabase;
	private final IBookmarksModificationsHandler bookmarksModificationsHandler;
	private final Queue<BookmarksModification> eventsQueue = new ConcurrentLinkedQueue<BookmarksModification>();
	private final IBookmarksListener bookmarksListener;
	private final long scheduleDelay;
	private final AtomicInteger previousSize = new AtomicInteger(0);
	private final AtomicInteger unhandledEventsCount = new AtomicInteger(0);
	private ScheduledExecutorService scheduledExecutorService;
	private final BookmarksModificationBatchHandlerJob job = new BookmarksModificationBatchHandlerJob();
	private ScheduledFuture scheduledFuture;

	public BackgroundBookmarksModificationsHandler(BookmarkDatabase bookmarkDatabase,
			IBookmarksModificationsHandler bookmarksModificationsHandler, long delay) {
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
	
	public void init() {
		scheduledExecutorService = Executors.newScheduledThreadPool(1);
		bookmarkDatabase.addListener(bookmarksListener);
	}

	public void close() {
		bookmarkDatabase.removeListener(bookmarksListener);
		scheduledExecutorService.shutdown();
		try {
			scheduledExecutorService.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	private class BookmarksModificationBatchHandlerJob implements Runnable {

		public void run() {
			if (previousSize.get() != eventsQueue.size()) {
				// don't handle modifications yet if there are new events
				previousSize.set(eventsQueue.size());
				schedule();
				return;
			}
			try {
				List<BookmarksModification> modifications = getBookmarksModifications();
				previousSize.set(0);
				if (modifications.size() == 0) {
					return;
				}
				bookmarksModificationsHandler.handle(modifications);
			} catch (BookmarksException e) {
				return;
			} finally {
				unhandledEventsCount.set(eventsQueue.size());
			}
		}

		private List<BookmarksModification> getBookmarksModifications() {
			List<BookmarksModification> list = new ArrayList<>();
			BookmarksModification event;
			while ((event = eventsQueue.poll()) != null) {
				list.add(event);
			}
			return list;
		}
	}

	public static interface IBookmarksModificationsHandler {

		public void handle(List<BookmarksModification> modifications)
				throws BookmarksException;

	}

}
