package mesfavoris.extensions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import mesfavoris.bookmarktype.*;
import mesfavoris.ui.details.IBookmarkDetailPart;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that manages bookmark type extensions and provides access to their providers.
 */
@Service
public final class BookmarkTypeExtensionManager {
    
    private final Map<String, BookmarkTypeExtension> bookmarkTypes = new HashMap<>();
    private final List<PrioritizedElement<IBookmarkPropertiesProvider>> allPropertiesProviders = new ArrayList<>();
    private final List<PrioritizedElement<IBookmarkLabelProvider>> allLabelProviders = new ArrayList<>();
    private final List<PrioritizedElement<IBookmarkLocationProvider>> allLocationProviders = new ArrayList<>();
    private final List<PrioritizedElement<IGotoBookmark>> allGotoBookmarkHandlers = new ArrayList<>();
    private final List<PrioritizedElement<IBookmarkMarkerAttributesProvider>> allMarkerAttributesProviders = new ArrayList<>();
    private final List<PrioritizedElement<IBookmarkDetailPart>> allDetailParts = new ArrayList<>();
    
    public BookmarkTypeExtensionManager() {
        loadExtensions();
        setupExtensionPointListener();
    }
    
    public static BookmarkTypeExtensionManager getInstance() {
        return ApplicationManager.getApplication().getService(BookmarkTypeExtensionManager.class);
    }
    
    /**
     * Get all registered bookmark type extensions
     */
    @NotNull
    public Collection<BookmarkTypeExtension> getAllBookmarkTypes() {
        return Collections.unmodifiableCollection(bookmarkTypes.values());
    }
    
    /**
     * Get a bookmark type by name
     */
    @NotNull
    public Optional<BookmarkTypeExtension> getBookmarkType(@NotNull String name) {
        return Optional.ofNullable(bookmarkTypes.get(name));
    }
    
    /**
     * Get all properties providers from all bookmark types, sorted by priority
     */
    @NotNull
    public List<IBookmarkPropertiesProvider> getAllPropertiesProviders() {
        return allPropertiesProviders.stream()
                .sorted()
                .map(PrioritizedElement::getElement)
                .collect(Collectors.toList());
    }

    /**
     * Get all label providers from all bookmark types, sorted by priority
     */
    @NotNull
    public List<IBookmarkLabelProvider> getAllLabelProviders() {
        return allLabelProviders.stream()
                .sorted()
                .map(PrioritizedElement::getElement)
                .collect(Collectors.toList());
    }

    /**
     * Get all location providers from all bookmark types, sorted by priority
     */
    @NotNull
    public List<IBookmarkLocationProvider> getAllLocationProviders() {
        return allLocationProviders.stream()
                .sorted()
                .map(PrioritizedElement::getElement)
                .collect(Collectors.toList());
    }

    /**
     * Get all goto bookmark handlers from all bookmark types, sorted by priority
     */
    @NotNull
    public List<IGotoBookmark> getAllGotoBookmarkHandlers() {
        return allGotoBookmarkHandlers.stream()
                .sorted()
                .map(PrioritizedElement::getElement)
                .collect(Collectors.toList());
    }

    /**
     * Get all marker attributes providers from all bookmark types, sorted by priority
     */
    @NotNull
    public List<IBookmarkMarkerAttributesProvider> getAllMarkerAttributesProviders() {
        return allMarkerAttributesProviders.stream()
                .sorted()
                .map(PrioritizedElement::getElement)
                .collect(Collectors.toList());
    }

    /**
     * Get all detail parts from all bookmark types, sorted by priority
     */
    @NotNull
    public List<IBookmarkDetailPart> getAllDetailParts() {
        return allDetailParts.stream()
                .sorted()
                .map(PrioritizedElement::getElement)
                .collect(Collectors.toList());
    }
    
    private void loadExtensions() {
        // Clear existing data
        bookmarkTypes.clear();
        allPropertiesProviders.clear();
        allLabelProviders.clear();
        allLocationProviders.clear();
        allGotoBookmarkHandlers.clear();
        allMarkerAttributesProviders.clear();
        allDetailParts.clear();
        
        // Load bookmark type extensions
        List<BookmarkTypeExtension> extensions = BookmarkTypeExtension.EP_NAME.getExtensionList();

        for (BookmarkTypeExtension extension : extensions) {
            bookmarkTypes.put(extension.getName(), extension);

            // Collect all elements with their priorities
            allPropertiesProviders.addAll(extension.getPropertiesProviders());
            allLabelProviders.addAll(extension.getLabelProviders());
            allLocationProviders.addAll(extension.getLocationProviders());
            allGotoBookmarkHandlers.addAll(extension.getGotoBookmarkHandlers());
            allMarkerAttributesProviders.addAll(extension.getMarkerAttributesProviders());
            allDetailParts.addAll(extension.getDetailParts());
        }
    }
    
    private void setupExtensionPointListener() {
        BookmarkTypeExtension.EP_NAME.addExtensionPointListener(new ExtensionPointListener<>() {
            @Override
            public void extensionAdded(@NotNull BookmarkTypeExtension extension, @NotNull PluginDescriptor pluginDescriptor) {
                loadExtensions(); // Reload all extensions
            }
            
            @Override
            public void extensionRemoved(@NotNull BookmarkTypeExtension extension, @NotNull PluginDescriptor pluginDescriptor) {
                loadExtensions(); // Reload all extensions
            }
        }, null);
    }
}
