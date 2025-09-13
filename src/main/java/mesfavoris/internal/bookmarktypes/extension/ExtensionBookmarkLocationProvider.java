package mesfavoris.internal.bookmarktypes.extension;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.extensions.BookmarkTypeExtensionManager;
import mesfavoris.internal.bookmarktypes.BookmarkLocationProvider;
import mesfavoris.model.Bookmark;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Location provider that delegates to BookmarkLocationProvider using providers from bookmark type extensions.
 * This provider creates a BookmarkLocationProvider instance with all registered location providers
 * from the extension system and delegates the method call to it.
 */
public class ExtensionBookmarkLocationProvider implements IBookmarkLocationProvider {

    private final BookmarkTypeExtensionManager extensionManager;

    public ExtensionBookmarkLocationProvider() {
        this.extensionManager = BookmarkTypeExtensionManager.getInstance();
    }

    public ExtensionBookmarkLocationProvider(@NotNull BookmarkTypeExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }

    @Override
    public IBookmarkLocation getBookmarkLocation(Project project, Bookmark bookmark, ProgressIndicator progress) {
        BookmarkLocationProvider locationProvider = createBookmarkLocationProvider();
        return locationProvider.getBookmarkLocation(project, bookmark, progress);
    }

    /**
     * Create a BookmarkLocationProvider instance with location providers from enabled bookmark types.
     *
     * @return a new BookmarkLocationProvider configured with enabled extension providers
     */
    private BookmarkLocationProvider createBookmarkLocationProvider() {
        List<IBookmarkLocationProvider> providers = extensionManager.getEnabledLocationProviders();
        return new BookmarkLocationProvider(providers);
    }
}
