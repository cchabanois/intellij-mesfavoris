package mesfavoris.internal.bookmarktypes.extension;

import mesfavoris.bookmarktype.BookmarkPropertyDescriptor;
import mesfavoris.bookmarktype.IBookmarkPropertyDescriptors;
import mesfavoris.bookmarktype.IDisabledBookmarkTypesProvider;
import mesfavoris.extensions.BookmarkTypeExtension;
import mesfavoris.extensions.BookmarkTypeExtensionManager;
import mesfavoris.internal.settings.bookmarktypes.BookmarkTypesStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Property descriptors provider that aggregates descriptors from all enabled bookmark type extensions.
 * This class collects property descriptors from all registered bookmark types in the extension system.
 */
public class ExtensionBookmarkPropertyDescriptors implements IBookmarkPropertyDescriptors {

    private final BookmarkTypeExtensionManager extensionManager;
    private final IDisabledBookmarkTypesProvider disabledTypesProvider;

    public ExtensionBookmarkPropertyDescriptors() {
        this.extensionManager = BookmarkTypeExtensionManager.getInstance();
        this.disabledTypesProvider = BookmarkTypesStore.getInstance();
    }

    public ExtensionBookmarkPropertyDescriptors(@NotNull BookmarkTypeExtensionManager extensionManager,
                                                 @NotNull IDisabledBookmarkTypesProvider disabledTypesProvider) {
        this.extensionManager = extensionManager;
        this.disabledTypesProvider = disabledTypesProvider;
    }

    @Override
    public BookmarkPropertyDescriptor getPropertyDescriptor(String propertyName) {
        return extensionManager.getAllBookmarkTypes().stream()
                .filter(extension -> disabledTypesProvider.isBookmarkTypeEnabled(extension.getName()))
                .map(extension -> extension.getPropertyDescriptor(propertyName))
                .filter(descriptor -> descriptor != null)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<BookmarkPropertyDescriptor> getPropertyDescriptors() {
        List<BookmarkPropertyDescriptor> allDescriptors = new ArrayList<>();

        extensionManager.getAllBookmarkTypes().stream()
                .filter(extension -> disabledTypesProvider.isBookmarkTypeEnabled(extension.getName()))
                .forEach(extension -> allDescriptors.addAll(extension.getPropertyDescriptors()));

        return allDescriptors;
    }
}

