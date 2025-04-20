package mesfavoris.internal.persistence;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.containers.ContainerUtil;
import mesfavoris.model.*;

import com.google.common.collect.ImmutableSet;

import mesfavoris.BookmarksException;
import mesfavoris.internal.jobs.BackgroundBookmarksModificationsHandler;
import mesfavoris.internal.jobs.BackgroundBookmarksModificationsHandler.IBookmarksModificationsHandler;
import mesfavoris.model.modification.BookmarkDeletedModification;
import mesfavoris.model.modification.BookmarkPropertiesModification;
import mesfavoris.model.modification.BookmarksAddedModification;
import mesfavoris.model.modification.BookmarksModification;
import mesfavoris.model.modification.BookmarksMovedModification;
import mesfavoris.persistence.IBookmarksDirtyStateListener;
import mesfavoris.persistence.IBookmarksDirtyStateTracker;

/**
 * Save bookmarks from a {@link BookmarkDatabase} when bookmarks are
 * added/modified/deleted ...
 * 
 * @author cchabanois
 *
 */
public class BookmarksAutoSaver implements IBookmarksDirtyStateTracker {
	private static final Logger LOG = Logger.getInstance(BookmarksAutoSaver.class);
	private static final int SAVE_DELAY = 2000;
	private final BookmarkDatabase bookmarkDatabase;
	private final BackgroundBookmarksModificationsHandler backgroundBookmarksModificationsHandler;
	private final LocalBookmarksSaver localBookmarksSaver;
    private final IBookmarksListener bookmarksListener;
	private final List<IBookmarksDirtyStateListener> listenerList = ContainerUtil.createLockFreeCopyOnWriteList();
	private final AtomicReference<Set<BookmarkId>> dirtyBookmarksRef = new AtomicReference<Set<BookmarkId>>(
			Collections.emptySet());

	public BookmarksAutoSaver(BookmarkDatabase bookmarkDatabase, LocalBookmarksSaver localBookmarksSaver) {
		this.bookmarkDatabase = bookmarkDatabase;
		this.localBookmarksSaver = localBookmarksSaver;
        this.backgroundBookmarksModificationsHandler = new BackgroundBookmarksModificationsHandler(bookmarkDatabase, new SaveModificationsHandler(), SAVE_DELAY);
		this.bookmarksListener = modifications -> {
			computeDirtyBookmarks();
			fireDirtyBookmarksChanged(getDirtyBookmarks());
		};
	}

	public void init() {
		backgroundBookmarksModificationsHandler.init();
		bookmarkDatabase.addListener(bookmarksListener);
	}

	public void close() {
		bookmarkDatabase.removeListener(bookmarksListener);
		backgroundBookmarksModificationsHandler.close();
	}

	@Override
	public Set<BookmarkId> getDirtyBookmarks() {
		return dirtyBookmarksRef.get();
	}

	private void computeDirtyBookmarks() {
		List<BookmarksModification> bookmarksModifications = backgroundBookmarksModificationsHandler
				.getUnhandledEvents();
		Set<BookmarkId> dirtyBookmarks = new HashSet<>();
		for (BookmarksModification bookmarksModification : bookmarksModifications) {
			if (bookmarksModification instanceof BookmarkDeletedModification bookmarkDeletedModification) {
                dirtyBookmarks.add(bookmarkDeletedModification.getBookmarkParentId());
			} else if (bookmarksModification instanceof BookmarkPropertiesModification bookmarkPropertiesModification) {
                dirtyBookmarks.add(bookmarkPropertiesModification.getBookmarkId());
			} else if (bookmarksModification instanceof BookmarksAddedModification bookmarksAddedModification) {
                dirtyBookmarks.add(bookmarksAddedModification.getParentId());
				dirtyBookmarks.addAll(bookmarksAddedModification.getBookmarks().stream()
						.map(Bookmark::getId).toList());
			} else if (bookmarksModification instanceof BookmarksMovedModification bookmarksMovedModification) {
                dirtyBookmarks.add(bookmarksMovedModification.getNewParentId());
			}
		}
		dirtyBookmarksRef.set(ImmutableSet.copyOf(dirtyBookmarks));
	}

	@Override
	public void addListener(IBookmarksDirtyStateListener listener) {
		listenerList.add(listener);
	}

	@Override
	public void removeListener(IBookmarksDirtyStateListener listener) {
		listenerList.remove(listener);
	}

	private void fireDirtyBookmarksChanged(Set<BookmarkId> dirtyBookmarks) {
		for (IBookmarksDirtyStateListener listener : listenerList) {
			try {
				listener.dirtyBookmarks(dirtyBookmarks);
			} catch(Exception|LinkageError|AssertionError e) {
				LOG.error("Error in bookmarks dirty state listener", e);
			}
		}
	}

	private class SaveModificationsHandler implements IBookmarksModificationsHandler {

		@Override
		public void handle(List<BookmarksModification> modifications)
				throws BookmarksException {
			try {
				BookmarksModification latestModification = modifications.get(modifications.size() - 1);
				BookmarksTree bookmarksTree = latestModification.getTargetTree();
				localBookmarksSaver.saveBookmarks(bookmarksTree);
			} finally {
				computeDirtyBookmarks();
				fireDirtyBookmarksChanged(getDirtyBookmarks());
			}
		}

	}

}
