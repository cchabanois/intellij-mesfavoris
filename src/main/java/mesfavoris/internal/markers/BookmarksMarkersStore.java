package mesfavoris.internal.markers;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.model.BookmarkId;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Persistent storage for bookmark markers
 */
@State(
    name = "BookmarkMarkers",
    storages = @Storage("mesfavoris.xml")
)
public class BookmarksMarkersStore implements PersistentStateComponent<Element>, IBookmarksMarkersStore {
    private final Map<BookmarkId, BookmarkMarker> bookmarkIdToMarkerMap = new HashMap<>();
    private final Map<VirtualFile, Set<BookmarkMarker>> fileToMarkerMap = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public BookmarkMarker put(BookmarkMarker bookmarkMarker) {
        lock.lock();
        try {
            BookmarkMarker previous = bookmarkIdToMarkerMap.put(bookmarkMarker.getBookmarkId(), bookmarkMarker);
            if (previous != null) {
                removeFromFileToMarkerMap(previous);
            }
            fileToMarkerMap.computeIfAbsent(bookmarkMarker.getResource(), k -> new HashSet<>())
                    .add(bookmarkMarker);
            return previous;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public BookmarkMarker remove(BookmarkId bookmarkId) {
        lock.lock();
        try {
            BookmarkMarker bookmarkMarker = bookmarkIdToMarkerMap.remove(bookmarkId);
            if (bookmarkMarker != null) {
                removeFromFileToMarkerMap(bookmarkMarker);
            }
            return bookmarkMarker;
        } finally {
            lock.unlock();
        }
    }

    private void removeFromFileToMarkerMap(BookmarkMarker bookmarkMarker) {
        Set<BookmarkMarker> markers = fileToMarkerMap.get(bookmarkMarker.getResource());
        if (markers != null) {
            markers.remove(bookmarkMarker);
            if (markers.isEmpty()) {
                fileToMarkerMap.remove(bookmarkMarker.getResource());
            }
        }
    }

    @Override
    public BookmarkMarker get(BookmarkId bookmarkId) {
        lock.lock();
        try {
            return bookmarkIdToMarkerMap.get(bookmarkId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<BookmarkMarker> get(VirtualFile file) {
        lock.lock();
        try {
            Set<BookmarkMarker> markers = fileToMarkerMap.get(file);
            return markers != null ? new ArrayList<>(markers) : Collections.emptyList();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Element getState() {
        lock.lock();
        try {
            Element container = new Element("BookmarkMarkers");
            for (BookmarkMarker bookmarkMarker : bookmarkIdToMarkerMap.values()) {
                Element bookmarkMarkerElement = new Element("bookmarkMarker");
                bookmarkMarkerElement.setAttribute("url", bookmarkMarker.getResource().getUrl());
                bookmarkMarkerElement.setAttribute("bookmarkId", bookmarkMarker.getBookmarkId().toString());
                bookmarkMarker.getAttributes().forEach((key, value) -> {
                    Element attributeElement = new Element("attribute");
                    attributeElement.setAttribute("name", key);
                    attributeElement.setAttribute("value", value);
                    bookmarkMarkerElement.addContent(attributeElement);
                });
                container.addContent(bookmarkMarkerElement);
            }
            return container;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void loadState(@NotNull Element state) {
        lock.lock();
        try {
            bookmarkIdToMarkerMap.clear();
            fileToMarkerMap.clear();
            for (Element bookmarkElement : state.getChildren("bookmarkMarker")) {
                String urlString = bookmarkElement.getAttributeValue("url");
                String bookmarkIdString = bookmarkElement.getAttributeValue("bookmarkId");
                Map<String, String> attributes = new HashMap<>();
                for (Element element : bookmarkElement.getChildren()) {
                    String name = element.getAttributeValue("name");
                    String value = element.getAttributeValue("value");
                    attributes.put(name, value);
                }
                if (urlString != null && bookmarkIdString != null) {
                    VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(urlString);
                    BookmarkId bookmarkId = new BookmarkId(bookmarkIdString);
                    if (virtualFile != null) {
                        BookmarkMarker bookmarkMarker = new BookmarkMarker(virtualFile, bookmarkId, attributes);
                        put(bookmarkMarker);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }


}
