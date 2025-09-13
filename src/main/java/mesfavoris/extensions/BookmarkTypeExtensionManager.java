package mesfavoris.extensions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.*;
import mesfavoris.commons.Pair;
import mesfavoris.internal.settings.bookmarktypes.BookmarkTypesStore;
import mesfavoris.ui.details.IBookmarkDetailPart;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service that manages bookmark type extensions and provides access to their providers.
 */
@Service
public final class BookmarkTypeExtensionManager {
    
    private final Map<String, BookmarkTypeExtension> bookmarkTypes = new HashMap<>();
    private final List<PrioritizedElement<Pair<String, IBookmarkPropertiesProvider>>> allPropertiesProviders = new ArrayList<>();
    private final List<PrioritizedElement<Pair<String, IBookmarkLabelProvider>>> allLabelProviders = new ArrayList<>();
    private final List<PrioritizedElement<Pair<String, IBookmarkLocationProvider>>> allLocationProviders = new ArrayList<>();
    private final List<PrioritizedElement<Pair<String, IGotoBookmark>>> allGotoBookmarkHandlers = new ArrayList<>();
    private final List<PrioritizedElement<Pair<String, IBookmarkMarkerAttributesProvider>>> allMarkerAttributesProviders = new ArrayList<>();
    private final List<PrioritizedElement<Pair<String, Function<Project, IBookmarkDetailPart>>>> allDetailPartProviders = new ArrayList<>();
    private final IDisabledBookmarkTypesProvider disabledTypesProvider;
    public BookmarkTypeExtensionManager() {
        loadExtensions();
        setupExtensionPointListener();
        disabledTypesProvider = BookmarkTypesStore.getInstance();
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
     * Get properties providers from enabled bookmark types, sorted by priority.
     */
    @NotNull
    public List<IBookmarkPropertiesProvider> getEnabledPropertiesProviders() {
        return filterEnabledProviders(allPropertiesProviders);
    }

    /**
     * Get label providers from enabled bookmark types, sorted by priority.
     */
    @NotNull
    public List<IBookmarkLabelProvider> getEnabledLabelProviders() {
        return filterEnabledProviders(allLabelProviders);
    }

    /**
     * Get location providers from enabled bookmark types, sorted by priority.
     */
    @NotNull
    public List<IBookmarkLocationProvider> getEnabledLocationProviders() {
        return filterEnabledProviders(allLocationProviders);
    }

    /**
     * Get goto bookmark handlers from enabled bookmark types, sorted by priority.
     */
    @NotNull
    public List<IGotoBookmark> getEnabledGotoBookmarkHandlers() {
        return filterEnabledProviders(allGotoBookmarkHandlers);
    }

    /**
     * Get marker attributes providers from enabled bookmark types, sorted by priority.
     */
    @NotNull
    public List<IBookmarkMarkerAttributesProvider> getEnabledMarkerAttributesProviders() {
        return filterEnabledProviders(allMarkerAttributesProviders);
    }

    /**
     * Get detail part providers from enabled bookmark types, sorted by priority.
     */
    @NotNull
    public List<Function<Project, IBookmarkDetailPart>> getEnabledDetailPartProviders() {
        return filterEnabledProviders(allDetailPartProviders);
    }

    /**
     * Create all detail parts for a given project, sorted by priority
     */
    @NotNull
    public List<IBookmarkDetailPart> createDetailParts(@NotNull Project project) {
        List<IBookmarkDetailPart> allParts = new ArrayList<>();

        // Create instances from enabled providers (functions)
        for (Pair<String, Function<Project, IBookmarkDetailPart>> pair : allDetailPartProviders.stream()
                .sorted()
                .map(PrioritizedElement::getElement)
                .toList()) {
            try {
                String bookmarkTypeName = pair.getFirst();
                Function<Project, IBookmarkDetailPart> provider = pair.getSecond();
                IBookmarkDetailPart instance = provider.apply(project);
                allParts.add(new DisableableBookmarkDetailPart(bookmarkTypeName, instance, disabledTypesProvider));
            } catch (Exception e) {
                // Log error but continue with other detail parts
                Logger.getInstance(BookmarkTypeExtensionManager.class)
                        .error("Failed to create detail part from provider", e);
            }
        }

        return allParts;
    }

    /**
     * Filter providers by enabled bookmark types and extract the actual providers
     */
    private <T> List<T> filterEnabledProviders(List<PrioritizedElement<Pair<String, T>>> providers) {
        IDisabledBookmarkTypesProvider disabledTypesProvider = BookmarkTypesStore.getInstance();

        return providers.stream()
                .filter(providerElement -> {
                    Pair<String, T> typedProvider = providerElement.getElement();
                    return disabledTypesProvider.isBookmarkTypeEnabled(typedProvider.getFirst());
                })
                .sorted()
                .map(providerElement -> providerElement.getElement().getSecond())
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
        allDetailPartProviders.clear();
        
        // Load bookmark type extensions
        List<BookmarkTypeExtension> extensions = BookmarkTypeExtension.EP_NAME.getExtensionList();

        for (BookmarkTypeExtension extension : extensions) {
            bookmarkTypes.put(extension.getName(), extension);

            // Collect all elements with their priorities, wrapping providers with bookmark type name
            String typeName = extension.getName();

            // Wrap all providers with bookmark type name
            for (PrioritizedElement<IBookmarkPropertiesProvider> providerElement : extension.getPropertiesProviders()) {
                Pair<String, IBookmarkPropertiesProvider> typedProvider =
                    Pair.of(typeName, providerElement.getElement());
                allPropertiesProviders.add(new PrioritizedElement<>(typedProvider, providerElement.getPriority()));
            }

            for (PrioritizedElement<IBookmarkLabelProvider> providerElement : extension.getLabelProviders()) {
                Pair<String, IBookmarkLabelProvider> typedProvider =
                    Pair.of(typeName, providerElement.getElement());
                allLabelProviders.add(new PrioritizedElement<>(typedProvider, providerElement.getPriority()));
            }

            for (PrioritizedElement<IBookmarkLocationProvider> providerElement : extension.getLocationProviders()) {
                Pair<String, IBookmarkLocationProvider> typedProvider =
                    Pair.of(typeName, providerElement.getElement());
                allLocationProviders.add(new PrioritizedElement<>(typedProvider, providerElement.getPriority()));
            }

            for (PrioritizedElement<IGotoBookmark> providerElement : extension.getGotoBookmarkHandlers()) {
                Pair<String, IGotoBookmark> typedProvider =
                    Pair.of(typeName, providerElement.getElement());
                allGotoBookmarkHandlers.add(new PrioritizedElement<>(typedProvider, providerElement.getPriority()));
            }

            for (PrioritizedElement<IBookmarkMarkerAttributesProvider> providerElement : extension.getMarkerAttributesProviders()) {
                Pair<String, IBookmarkMarkerAttributesProvider> typedProvider =
                    Pair.of(typeName, providerElement.getElement());
                allMarkerAttributesProviders.add(new PrioritizedElement<>(typedProvider, providerElement.getPriority()));
            }

            for (PrioritizedElement<Function<Project, IBookmarkDetailPart>> providerElement : extension.getDetailPartProviders()) {
                Pair<String, Function<Project, IBookmarkDetailPart>> typedProvider =
                    Pair.of(typeName, providerElement.getElement());
                allDetailPartProviders.add(new PrioritizedElement<>(typedProvider, providerElement.getPriority()));
            }
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
