package mesfavoris.internal.persistence;

import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.containers.ContainerUtil;
import mesfavoris.BookmarksException;
import mesfavoris.internal.jobs.BackgroundBookmarksModificationsHandler;
import mesfavoris.internal.jobs.BackgroundBookmarksModificationsHandler.IBookmarksModificationsHandler;
import mesfavoris.model.*;
import mesfavoris.model.modification.*;
import mesfavoris.persistence.IBookmarksDirtyStateListener;
import mesfavoris.persistence.IBookmarksDirtyStateTracker;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Save bookmarks from a {@link BookmarkDatabase} when bookmarks are
 * added/modified/deleted ...
 *
 * @author cchabanois
 */
public class BookmarksAutoSaver implements IBookmarksDirtyStateTracker, Disposable {
    private static final Logger LOG = Logger.getInstance(BookmarksAutoSaver.class);
    private static final int SAVE_DELAY = 2000;
    private final BookmarkDatabase bookmarkDatabase;
    private final BackgroundBookmarksModificationsHandler backgroundBookmarksModificationsHandler;
    private final LocalBookmarksSaver localBookmarksSaver;
    private final RemoteBookmarksSaver remoteBookmarksSaver;
    private final IBookmarksListener bookmarksListener;
    private final List<IBookmarksDirtyStateListener> listenerList = ContainerUtil.createLockFreeCopyOnWriteList();
    private final AtomicReference<Set<BookmarkId>> dirtyBookmarksRef = new AtomicReference<>(Collections.emptySet());

    public BookmarksAutoSaver(Project project, BookmarkDatabase bookmarkDatabase, LocalBookmarksSaver localBookmarksSaver, RemoteBookmarksSaver remoteBookmarksSaver) {
        this.bookmarkDatabase = bookmarkDatabase;
        this.localBookmarksSaver = localBookmarksSaver;
        this.remoteBookmarksSaver = remoteBookmarksSaver;
        this.backgroundBookmarksModificationsHandler = new BackgroundBookmarksModificationsHandler(project, bookmarkDatabase, new SaveModificationsHandler(), SAVE_DELAY);
        this.bookmarksListener = modifications -> {
            computeDirtyBookmarks();
            fireDirtyBookmarksChanged(getDirtyBookmarks());
        };
    }

    public void init() {
        backgroundBookmarksModificationsHandler.init();
        Disposer.register(this, backgroundBookmarksModificationsHandler);
        bookmarkDatabase.addListener(bookmarksListener);
    }

    @Override
    public void dispose() {
        bookmarkDatabase.removeListener(bookmarksListener);
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
            } catch (Exception | LinkageError | AssertionError e) {
                LOG.error("Error in bookmarks dirty state listener", e);
            }
        }
    }

    public BackgroundBookmarksModificationsHandler getBackgroundBookmarksModificationsHandler() {
        return backgroundBookmarksModificationsHandler;
    }

    private class SaveModificationsHandler implements IBookmarksModificationsHandler {

        @Override
        public void handle(List<BookmarksModification> modifications, @NotNull ProgressIndicator progressIndicator)
                throws BookmarksException {
            try {
                BookmarksModification latestModification = modifications.getLast();
                BookmarksTree bookmarksTree = latestModification.getTargetTree();
                localBookmarksSaver.saveBookmarks(bookmarksTree);
                remoteBookmarksSaver.applyModificationsToRemoteBookmarksStores(modifications, progressIndicator);
            } finally {
                computeDirtyBookmarks();
                fireDirtyBookmarksChanged(getDirtyBookmarks());
            }
        }

    }

}
