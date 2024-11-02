package mesfavoris.internal.markers;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.containers.MultiMap;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.model.BookmarkId;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class BookmarksMarkersMap implements PersistentStateComponent<Element> {
    private final Map<BookmarkId, BookmarkMarker> bookmarkIdToMarkerMap = new HashMap<>();
    private final MultiMap<VirtualFile, BookmarkMarker> fileToMarkerMap = new MultiMap<VirtualFile, BookmarkMarker>() {
        @Override
        protected @NotNull Collection<BookmarkMarker> createCollection() {
            return new HashSet<>();
        }

        @Override
        protected @NotNull Collection<BookmarkMarker> createEmptyCollection() {
            return Collections.emptySet();
        }
    };
    private final ReentrantLock lock = new ReentrantLock();

    public BookmarkMarker put(BookmarkMarker bookmarkMarker) {
        lock.lock();
        try {
            BookmarkMarker previous = bookmarkIdToMarkerMap.put(bookmarkMarker.getBookmarkId(), bookmarkMarker);
            fileToMarkerMap.putValue(bookmarkMarker.getResource(), bookmarkMarker);
            return previous;
        } finally {
            lock.unlock();
        }
    }

    public BookmarkMarker remove(BookmarkId bookmarkId) {
        lock.lock();
        BookmarkMarker bookmarkMarker;
        try {
            bookmarkMarker = bookmarkIdToMarkerMap.remove(bookmarkId);
            if (bookmarkMarker != null) {
                fileToMarkerMap.remove(bookmarkMarker.getResource(), bookmarkMarker);
            }
        } finally {
            lock.unlock();
        }
        return bookmarkMarker;
    }

    public BookmarkMarker get(BookmarkId bookmarkId) {
        lock.lock();
        try {
            return bookmarkIdToMarkerMap.get(bookmarkId);
        } finally {
            lock.unlock();
        }
    }

    public List<BookmarkMarker> get(VirtualFile file) {
        lock.lock();
        try {
            return new ArrayList<>(fileToMarkerMap.get(file));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Element getState() {
        lock.lock();
        try {
            Element container = new Element("BookmarkManager");
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
            List<BookmarkMarker> result = new ArrayList<>();
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
