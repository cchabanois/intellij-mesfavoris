package mesfavoris.internal.bookmarktypes.extension;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.bookmarktype.IBookmarkPropertiesProvider;
import mesfavoris.extensions.BookmarkTypeExtensionManager;
import mesfavoris.internal.bookmarktypes.BookmarkPropertiesProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Properties provider that delegates to BookmarkPropertiesProvider using providers from bookmark type extensions.
 * This provider creates a BookmarkPropertiesProvider instance with all registered properties providers
 * from the extension system and delegates the method call to it.
 */
public class ExtensionBookmarkPropertiesProvider implements IBookmarkPropertiesProvider {

    private final BookmarkTypeExtensionManager extensionManager;

    public ExtensionBookmarkPropertiesProvider() {
        this.extensionManager = BookmarkTypeExtensionManager.getInstance();
    }

    public ExtensionBookmarkPropertiesProvider(@NotNull BookmarkTypeExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }

    @Override
    public void addBookmarkProperties(Map<String, String> bookmarkProperties, DataContext dataContext, ProgressIndicator progress) {
        BookmarkPropertiesProvider propertiesProvider = createBookmarkPropertiesProvider();
        propertiesProvider.addBookmarkProperties(bookmarkProperties, dataContext, progress);
    }

    /**
     * Create a BookmarkPropertiesProvider instance with properties providers from enabled bookmark types.
     *
     * @return a new BookmarkPropertiesProvider configured with enabled extension providers
     */
    private BookmarkPropertiesProvider createBookmarkPropertiesProvider() {
        List<IBookmarkPropertiesProvider> providers = extensionManager.getEnabledPropertiesProviders();
        return new BookmarkPropertiesProvider(providers);
    }
}
