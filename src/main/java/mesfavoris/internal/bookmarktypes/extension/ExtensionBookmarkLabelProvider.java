package mesfavoris.internal.bookmarktypes.extension;

import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.extensions.BookmarkTypeExtensionManager;
import mesfavoris.internal.bookmarktypes.BookmarkLabelProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.ui.renderers.StyledString;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * Label provider that delegates to BookmarkLabelProvider using providers from bookmark type extensions.
 * This provider creates a BookmarkLabelProvider instance with all registered label providers
 * from the extension system and delegates each method call to it.
 */
public class ExtensionBookmarkLabelProvider implements IBookmarkLabelProvider {

    private final BookmarkTypeExtensionManager extensionManager;

    public ExtensionBookmarkLabelProvider() {
        this.extensionManager = BookmarkTypeExtensionManager.getInstance();
    }

    public ExtensionBookmarkLabelProvider(@NotNull BookmarkTypeExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }

    @Override
    public boolean canHandle(Context context, Bookmark bookmark) {
        BookmarkLabelProvider labelProvider = createBookmarkLabelProvider();
        return labelProvider.canHandle(context, bookmark);
    }

    @Override
    public StyledString getStyledText(Context context, Bookmark bookmark) {
        BookmarkLabelProvider labelProvider = createBookmarkLabelProvider();
        return labelProvider.getStyledText(context, bookmark);
    }

    @Override
    public Icon getIcon(Context context, Bookmark bookmark) {
        BookmarkLabelProvider labelProvider = createBookmarkLabelProvider();
        return labelProvider.getIcon(context, bookmark);
    }

    /**
     * Create a BookmarkLabelProvider instance with label providers from enabled bookmark types.
     *
     * @return a new BookmarkLabelProvider configured with enabled extension providers
     */
    private BookmarkLabelProvider createBookmarkLabelProvider() {
        List<IBookmarkLabelProvider> providers = extensionManager.getEnabledLabelProviders();
        return new BookmarkLabelProvider(providers);
    }
}
