package mesfavoris.remote;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for remote bookmarks store extensions.
 * Provides access to all registered remote bookmarks stores and manages their lifecycle.
 * This is a project-level service that creates and manages store instances for the project.
 */
@Service(Service.Level.PROJECT)
public final class RemoteBookmarksStoreExtensionManager {
    private static final Logger LOG = Logger.getInstance(RemoteBookmarksStoreExtensionManager.class);

    private final Project project;
    private final Map<String, RemoteBookmarksStoreExtension> extensions = new ConcurrentHashMap<>();
    private final Map<String, IRemoteBookmarksStore> stores = new ConcurrentHashMap<>();

    public RemoteBookmarksStoreExtensionManager(@NotNull Project project) {
        this.project = project;
        loadExtensions();
        setupExtensionPointListener();
    }

    /**
     * Get all remote bookmarks store instances for this project
     */
    @NotNull
    public List<IRemoteBookmarksStore> getStores() {
        return new ArrayList<>(stores.values());
    }

    /**
     * Get a remote bookmarks store instance by ID
     */
    @NotNull
    public Optional<IRemoteBookmarksStore> getStore(@NotNull String id) {
        return Optional.ofNullable(stores.get(id));
    }

    /**
     * Get a remote bookmarks store extension by ID
     */
    @NotNull
    public Optional<RemoteBookmarksStoreExtension> getExtension(@NotNull String id) {
        return Optional.ofNullable(extensions.get(id));
    }

    private void createStoreForExtension(@NotNull RemoteBookmarksStoreExtension extension) {
        try {
            AbstractRemoteBookmarksStore store = extension.createStore(project);
            // Initialize the store with its descriptor
            RemoteBookmarksStoreDescriptor descriptor = new RemoteBookmarksStoreDescriptor(
                    extension.getId(),
                    extension.getLabel(),
                    extension.getIcon(),
                    extension.getOverlayIcon()
            );
            store.init(descriptor);
            stores.put(extension.getId(), store);
        } catch (Exception e) {
            LOG.error("Failed to create remote bookmarks store for extension: " + extension.getId(), e);
        }
    }

    private void loadExtensions() {
        extensions.clear();
        stores.clear();
        for (RemoteBookmarksStoreExtension extension : RemoteBookmarksStoreExtension.EP_NAME.getExtensionList()) {
            registerExtension(extension);
        }
    }

    private void registerExtension(@NotNull RemoteBookmarksStoreExtension extension) {
        String id = extension.getId();
        if (extensions.containsKey(id)) {
            LOG.warn("Remote bookmarks store extension with ID '" + id + "' is already registered. Skipping.");
            return;
        }
        extensions.put(id, extension);
        createStoreForExtension(extension);
        LOG.info("Registered remote bookmarks store extension: " + id);
    }

    private void unregisterExtension(@NotNull RemoteBookmarksStoreExtension extension) {
        String id = extension.getId();
        extensions.remove(id);
        stores.remove(id);
        LOG.info("Unregistered remote bookmarks store extension: " + id);
    }

    private void setupExtensionPointListener() {
        RemoteBookmarksStoreExtension.EP_NAME.addExtensionPointListener(new ExtensionPointListener<>() {
            @Override
            public void extensionAdded(@NotNull RemoteBookmarksStoreExtension extension, @NotNull PluginDescriptor pluginDescriptor) {
                registerExtension(extension);
            }

            @Override
            public void extensionRemoved(@NotNull RemoteBookmarksStoreExtension extension, @NotNull PluginDescriptor pluginDescriptor) {
                unregisterExtension(extension);
            }
        }, null);
    }
}

