package mesfavoris.internal.markers;

import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import mesfavoris.IBookmarksMarkers;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.bookmarktype.IBookmarkMarkerAttributesProvider;
import mesfavoris.internal.jobs.BackgroundBookmarksModificationsHandler;
import mesfavoris.internal.markers.highlighters.BookmarksHighlightersListener;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.modification.BookmarkDeletedModification;
import mesfavoris.model.modification.BookmarkPropertiesModification;
import mesfavoris.model.modification.BookmarksAddedModification;
import mesfavoris.model.modification.BookmarksModification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookmarksMarkers implements IBookmarksMarkers, Disposable {
    private final Project project;
    private final BookmarkDatabase bookmarkDatabase;
    private final IBookmarkMarkerAttributesProvider bookmarkMarkerAttributesProvider;
    private final BackgroundBookmarksModificationsHandler backgroundBookmarksModificationsHandler;
    private final IBookmarksMarkersStore bookmarkMarkersStore;

    public BookmarksMarkers(Project project, BookmarkDatabase bookmarkDatabase, IBookmarksMarkersStore bookmarksMarkersStore,
                            IBookmarkMarkerAttributesProvider bookmarkMarkerAttributesProvider) {
        this.project = project;
        this.bookmarkDatabase = bookmarkDatabase;
        this.bookmarkMarkerAttributesProvider = bookmarkMarkerAttributesProvider;
        this.backgroundBookmarksModificationsHandler = new BackgroundBookmarksModificationsHandler(bookmarkDatabase, new BookmarksModificationsHandler(), 200);
        this.bookmarkMarkersStore = bookmarksMarkersStore;
    }

    public void init() {
        backgroundBookmarksModificationsHandler.init();
        MessageBusConnection messageBusConnection = project.getMessageBus().connect(this);
        messageBusConnection.subscribe(BookmarksHighlightersListener.TOPIC, new BookmarksHighlightersListenerImpl());
    }

    @Override
    public void dispose() {
        backgroundBookmarksModificationsHandler.close();
    }

    private void handleBookmarksModificationEvent(BookmarksModification event) {
        if (event instanceof BookmarkDeletedModification bookmarkDeletedModification) {
            List<Bookmark> deletedBookmarks = Lists.newArrayList(bookmarkDeletedModification.getDeletedBookmarks());
            deletedBookmarks.forEach(b -> deleteMarker(b.getId()));
        } else if (event instanceof BookmarksAddedModification bookmarksAddedModification) {
            bookmarksAddedModification.getBookmarks().forEach(this::createOrUpdateMarker);
        } else if (event instanceof BookmarkPropertiesModification bookmarkPropertiesModification) {
            createOrUpdateMarker(bookmarkPropertiesModification.getTargetTree()
                    .getBookmark(bookmarkPropertiesModification.getBookmarkId()));
        }
    }

    private void createOrUpdateMarker(Bookmark bookmarkAdded) {
        BookmarkMarker bookmarkMarker = bookmarkMarkerAttributesProvider.getMarkerDescriptor(project, bookmarkAdded, new EmptyProgressIndicator());
        if (bookmarkMarker == null) {
            BookmarkMarker previous = bookmarkMarkersStore.remove(bookmarkAdded.getId());
            if (previous != null) {
                project.getMessageBus().syncPublisher(BookmarksMarkersListener.TOPIC).bookmarkMarkerDeleted(previous);
            }
        } else {
            BookmarkMarker previous = bookmarkMarkersStore.put(bookmarkMarker);
            if (previous != null) {
                project.getMessageBus().syncPublisher(BookmarksMarkersListener.TOPIC).bookmarkMarkerUpdated(previous, bookmarkMarker);
            } else {
                project.getMessageBus().syncPublisher(BookmarksMarkersListener.TOPIC).bookmarkMarkerAdded(bookmarkMarker);
            }
        }
    }

    @Override
    public void deleteMarker(BookmarkId bookmarkId) {
        BookmarkMarker bookmarkMarker = bookmarkMarkersStore.remove(bookmarkId);
        if (bookmarkMarker != null) {
            project.getMessageBus().syncPublisher(BookmarksMarkersListener.TOPIC).bookmarkMarkerDeleted(bookmarkMarker);
        }
    }

    @Override
    public BookmarkMarker getMarker(BookmarkId bookmarkId) {
        return bookmarkMarkersStore.get(bookmarkId);
    }

    @Override
    public List<BookmarkMarker> getMarkers(VirtualFile file) {
        return bookmarkMarkersStore.get(file);
    }

    @Override
    public void refreshMarker(BookmarkId bookmarkId) {
        Bookmark bookmark = bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId);
        if (bookmark == null) {
            deleteMarker(bookmarkId);
        } else {
            createOrUpdateMarker(bookmark);
        }
    }

    private class BookmarksModificationsHandler implements BackgroundBookmarksModificationsHandler.IBookmarksModificationsHandler {

        @Override
        public void handle(List<BookmarksModification> modifications) {
            for (BookmarksModification modification : modifications) {
                handleBookmarksModificationEvent(modification);
            }
        }

    }

    private class BookmarksHighlightersListenerImpl implements BookmarksHighlightersListener {

        @Override
        public void bookmarkHighlighterDeleted(List<BookmarkId> bookmarkIds) {
            for (BookmarkId bookmarkId : bookmarkIds) {
                deleteMarker(bookmarkId);
            }
        }

        @Override
        public void bookmarkHighlighterMoved(RangeHighlighterEx bookmarkHighlighter) {
            int newLineNumber = bookmarkHighlighter.getDocument().getLineNumber(bookmarkHighlighter.getStartOffset());
            List<BookmarkId> bookmarkIds = this.getBookmarkIds(bookmarkHighlighter);
            if (bookmarkIds == null) {
                return;
            }
            // Update all bookmarks on this line
            for (BookmarkId bookmarkId : bookmarkIds) {
                BookmarkMarker previousBookmarkMarker = bookmarkMarkersStore.get(bookmarkId);
                if (previousBookmarkMarker != null && previousBookmarkMarker.getLineNumber() != newLineNumber) {
                    Map<String, String> newAttributes = new HashMap<>(previousBookmarkMarker.getAttributes());
                    newAttributes.put(BookmarkMarker.LINE_NUMBER, Integer.toString(newLineNumber));
                    BookmarkMarker newBookmarkMarker = new BookmarkMarker(previousBookmarkMarker.getResource(), previousBookmarkMarker.getBookmarkId(), newAttributes);
                    bookmarkMarkersStore.put(newBookmarkMarker);
                    project.getMessageBus().syncPublisher(BookmarksMarkersListener.TOPIC).bookmarkMarkerUpdated(previousBookmarkMarker, newBookmarkMarker);
                }
            }
        }
    }

    public interface BookmarksMarkersListener {
        Topic<BookmarksMarkersListener> TOPIC = Topic.create("BookmarksMarkersListener", BookmarksMarkersListener.class);

        void bookmarkMarkerDeleted(BookmarkMarker bookmarkMarker);

        void bookmarkMarkerAdded(BookmarkMarker bookmarkMarker);

        void bookmarkMarkerUpdated(BookmarkMarker previous, BookmarkMarker bookmarkMarker);

    }

}
