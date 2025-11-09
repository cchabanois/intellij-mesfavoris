package mesfavoris.gdrive.mappings;

import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.containers.ContainerUtil;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.IBookmarksListener;
import mesfavoris.model.modification.BookmarkDeletedModification;
import mesfavoris.model.modification.BookmarksModification;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Store mappings between bookmark folders and remote files that are storing
 * them
 *
 * @author cchabanois
 *
 */
@Service(Service.Level.PROJECT)
@State(name = "BookmarkMappings", storages = @Storage("mesfavoris-gdrive-mappings.xml"))
public final class BookmarkMappingsStore implements IBookmarksListener, IBookmarkMappings, PersistentStateComponent<Element> {
	private static final Logger LOG = Logger.getInstance(BookmarkMappingsStore.class);
	private static final String PROPERTY_FILE_ID = "fileId";

	private final Map<BookmarkId, BookmarkMapping> mappings = new ConcurrentHashMap<>();
	private final List<IBookmarkMappingsListener> listenerList = ContainerUtil.createLockFreeCopyOnWriteList();

	public BookmarkMappingsStore() {
	}

	public void add(BookmarkId bookmarkFolderId, String fileId,  Map<String, String> properties) {
		if (add(new BookmarkMapping(bookmarkFolderId, fileId, properties))) {
			fireMappingAdded(bookmarkFolderId);
		}
	}

	private boolean add(BookmarkMapping bookmarkMapping) {
		return mappings.put(bookmarkMapping.getBookmarkFolderId(), bookmarkMapping) == null;
	}

	private void replace(BookmarkMapping bookmarkMapping) {
		mappings.replace(bookmarkMapping.getBookmarkFolderId(), bookmarkMapping);
	}

	public void update(String fileId, Map<String,String> properties) {
		Optional<BookmarkMapping> mapping = getMapping(fileId);
		if (mapping.isEmpty()) {
			return;
		}
		if (!properties.equals(mapping.get().getProperties())) {
			replace(new BookmarkMapping(mapping.get().getBookmarkFolderId(), mapping.get().getFileId(), properties));
		}
	}

	public Optional<BookmarkMapping> getMapping(BookmarkId bookmarkFolderId) {
		return mappings.values().stream().filter(mapping -> mapping.getBookmarkFolderId().equals(bookmarkFolderId))
				.findAny();
	}

	public Optional<BookmarkMapping> getMapping(String fileId) {
		return mappings.values().stream().filter(mapping -> mapping.getFileId().equals(fileId)).findAny();
	}

	public Set<BookmarkMapping> getMappings() {
		return ImmutableSet.copyOf(mappings.values());
	}

	public void remove(BookmarkId bookmarkFolderId) {
		if (mappings.remove(bookmarkFolderId) != null) {
			fireMappingRemoved(bookmarkFolderId);
		}
	}

	@Override
	public void bookmarksModified(List<BookmarksModification> modifications) {
		Set<BookmarkMapping> mappingsToRemove = modifications.stream()
				.filter(modification -> modification instanceof BookmarkDeletedModification)
				.map(modification -> (BookmarkDeletedModification) modification)
				.map(this::getDeletedMappings)
				.reduce(new HashSet<>(), (mappingsSet, modificationMappingsSet) -> {
					mappingsSet.addAll(modificationMappingsSet);
					return mappingsSet;
				});
		mappingsToRemove.forEach(mapping -> remove(mapping.getBookmarkFolderId()));
	}

	private Set<BookmarkMapping> getDeletedMappings(BookmarkDeletedModification modification) {
		return mappings.values().stream()
				.filter(mapping -> modification.getTargetTree().getBookmark(mapping.getBookmarkFolderId()) == null)
				.collect(Collectors.toSet());
	}

	public void addListener(IBookmarkMappingsListener listener) {
		listenerList.add(listener);
	}

	public void removeListener(IBookmarkMappingsListener listener) {
		listenerList.remove(listener);
	}

	private void fireMappingAdded(BookmarkId bookmarkFolderId) {
		for (IBookmarkMappingsListener listener : listenerList) {
			try {
				listener.mappingAdded(bookmarkFolderId);
			} catch (Exception e) {
				LOG.error("Error when mapping added", e);
			}
		}
	}

	private void fireMappingRemoved(BookmarkId bookmarkFolderId) {
		for (IBookmarkMappingsListener listener : listenerList) {
			try {
				listener.mappingRemoved(bookmarkFolderId);
			} catch (Exception e) {
				LOG.error("Error when mapping removed", e);
			}
		}
	}

	@Nullable
	@Override
	public Element getState() {
		Element container = new Element("BookmarkMappings");
		for (BookmarkMapping mapping : mappings.values()) {
			Element mappingElement = new Element("mapping");
			mappingElement.setAttribute("bookmarkFolderId", mapping.getBookmarkFolderId().toString());
			mappingElement.setAttribute(PROPERTY_FILE_ID, mapping.getFileId());

			// Save properties
			for (Map.Entry<String, String> entry : mapping.getProperties().entrySet()) {
				Element propertyElement = new Element("property");
				propertyElement.setAttribute("name", entry.getKey());
				propertyElement.setAttribute("value", entry.getValue());
				mappingElement.addContent(propertyElement);
			}

			container.addContent(mappingElement);
		}
		return container;
	}

	@Override
	public void loadState(@NotNull Element state) {
		mappings.clear();
		for (Element mappingElement : state.getChildren("mapping")) {
			String bookmarkFolderIdString = mappingElement.getAttributeValue("bookmarkFolderId");
			String fileId = mappingElement.getAttributeValue(PROPERTY_FILE_ID);

			if (bookmarkFolderIdString != null && fileId != null) {
				Map<String, String> properties = new HashMap<>();
				for (Element propertyElement : mappingElement.getChildren("property")) {
					String name = propertyElement.getAttributeValue("name");
					String value = propertyElement.getAttributeValue("value");
					if (name != null && value != null) {
						properties.put(name, value);
					}
				}

				BookmarkId bookmarkFolderId = new BookmarkId(bookmarkFolderIdString);
				mappings.put(bookmarkFolderId, new BookmarkMapping(bookmarkFolderId, fileId, properties));
			}
		}
	}

}
