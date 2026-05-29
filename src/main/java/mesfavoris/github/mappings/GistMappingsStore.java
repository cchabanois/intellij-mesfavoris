package mesfavoris.github.mappings;

import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.IBookmarksListener;
import mesfavoris.model.modification.BookmarkDeletedModification;
import mesfavoris.model.modification.BookmarksModification;
import mesfavoris.service.IBookmarksService;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Persists the BookmarkFolder → Gist ID mappings to {@code mesfavoris.xml}.
 * Also listens to {@code BookmarkDatabase} to automatically remove mappings when their
 * corresponding folder is deleted.
 */
@Service(Service.Level.PROJECT)
@State(name = "GistMappings", storages = @Storage("mesfavoris.xml"))
public final class GistMappingsStore
        implements IBookmarksListener, IGistMappings, PersistentStateComponent<Element> {

    private static final Logger LOG = Logger.getInstance(GistMappingsStore.class);
    private static final String ATTR_GIST_ID = "gistId";

    private final Map<BookmarkId, GistMapping> mappings = new ConcurrentHashMap<>();
    private final Project project;
    private boolean initialized = false;

    public GistMappingsStore(@NotNull Project project) {
        this.project = project;
    }

    public synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        IBookmarksService bookmarksService = project.getService(IBookmarksService.class);
        if (bookmarksService != null) {
            bookmarksService.getBookmarkDatabase().addListener(this);
        }
    }

    public void add(BookmarkId bookmarkFolderId, String gistId, Map<String, String> properties) {
        if (mappings.put(bookmarkFolderId, new GistMapping(bookmarkFolderId, gistId, properties)) == null) {
            fireMappingAdded(bookmarkFolderId);
        }
    }

    public void update(String gistId, Map<String, String> properties) {
        Optional<GistMapping> mapping = getMapping(gistId);
        if (mapping.isEmpty()) {
            return;
        }
        GistMapping existing = mapping.get();
        if (!properties.equals(existing.getProperties())) {
            mappings.replace(existing.getBookmarkFolderId(),
                    new GistMapping(existing.getBookmarkFolderId(), gistId, properties));
        }
    }

    public void remove(BookmarkId bookmarkFolderId) {
        if (mappings.remove(bookmarkFolderId) != null) {
            fireMappingRemoved(bookmarkFolderId);
        }
    }

    @Override
    public Optional<GistMapping> getMapping(BookmarkId bookmarkFolderId) {
        return Optional.ofNullable(mappings.get(bookmarkFolderId));
    }

    @Override
    public Optional<GistMapping> getMapping(String gistId) {
        return mappings.values().stream()
                .filter(m -> m.getGistId().equals(gistId))
                .findAny();
    }

    @Override
    public Set<GistMapping> getMappings() {
        return ImmutableSet.copyOf(mappings.values());
    }

    @Override
    public void bookmarksModified(List<BookmarksModification> modifications) {
        Set<GistMapping> toRemove = modifications.stream()
                .filter(m -> m instanceof BookmarkDeletedModification)
                .map(m -> (BookmarkDeletedModification) m)
                .flatMap(m -> mappings.values().stream()
                        .filter(mapping -> m.getTargetTree().getBookmark(mapping.getBookmarkFolderId()) == null))
                .collect(Collectors.toSet());
        toRemove.forEach(m -> remove(m.getBookmarkFolderId()));
    }

    private void fireMappingAdded(BookmarkId bookmarkFolderId) {
        project.getMessageBus().syncPublisher(IGistMappingsListener.TOPIC).mappingAdded(bookmarkFolderId);
    }

    private void fireMappingRemoved(BookmarkId bookmarkFolderId) {
        project.getMessageBus().syncPublisher(IGistMappingsListener.TOPIC).mappingRemoved(bookmarkFolderId);
    }

    @Nullable
    @Override
    public Element getState() {
        Element container = new Element("GistMappings");
        for (GistMapping mapping : mappings.values()) {
            Element mappingElement = new Element("mapping");
            mappingElement.setAttribute("bookmarkFolderId", mapping.getBookmarkFolderId().toString());
            mappingElement.setAttribute(ATTR_GIST_ID, mapping.getGistId());
            for (Map.Entry<String, String> entry : mapping.getProperties().entrySet()) {
                Element property = new Element("property");
                property.setAttribute("name", entry.getKey());
                property.setAttribute("value", entry.getValue());
                mappingElement.addContent(property);
            }
            container.addContent(mappingElement);
        }
        return container;
    }

    @Override
    public void loadState(@NotNull Element state) {
        mappings.clear();
        for (Element mappingElement : state.getChildren("mapping")) {
            String bookmarkFolderIdStr = mappingElement.getAttributeValue("bookmarkFolderId");
            String gistId = mappingElement.getAttributeValue(ATTR_GIST_ID);
            if (bookmarkFolderIdStr == null || gistId == null) {
                continue;
            }
            Map<String, String> properties = new HashMap<>();
            for (Element property : mappingElement.getChildren("property")) {
                String name = property.getAttributeValue("name");
                String value = property.getAttributeValue("value");
                if (name != null && value != null) {
                    properties.put(name, value);
                }
            }
            BookmarkId bookmarkFolderId = new BookmarkId(bookmarkFolderIdStr);
            mappings.put(bookmarkFolderId, new GistMapping(bookmarkFolderId, gistId, properties));
        }
    }

    public static class Initializer implements StartupActivity.DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            GistMappingsStore store = project.getService(GistMappingsStore.class);
            if (store != null) {
                store.init();
            }
        }
    }
}
