package mesfavoris.internal.bookmarktypes.extension;

import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IGotoBookmark;
import mesfavoris.extensions.BookmarkTypeExtensionManager;
import mesfavoris.internal.bookmarktypes.GotoBookmark;
import mesfavoris.model.Bookmark;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Goto bookmark handler that delegates to GotoBookmark using handlers from bookmark type extensions.
 * This handler creates a GotoBookmark instance with all registered goto bookmark handlers
 * from the extension system and delegates the method call to it.
 */
public class ExtensionGotoBookmark implements IGotoBookmark {

    private final BookmarkTypeExtensionManager extensionManager;

    public ExtensionGotoBookmark() {
        this.extensionManager = BookmarkTypeExtensionManager.getInstance();
    }

    public ExtensionGotoBookmark(@NotNull BookmarkTypeExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }

    @Override
    public boolean gotoBookmark(Project project, Bookmark bookmark, IBookmarkLocation bookmarkLocation) {
        GotoBookmark gotoBookmark = createGotoBookmark();
        return gotoBookmark.gotoBookmark(project, bookmark, bookmarkLocation);
    }

    /**
     * Create a GotoBookmark instance with goto bookmark handlers from enabled bookmark types.
     *
     * @return a new GotoBookmark configured with enabled extension handlers
     */
    private GotoBookmark createGotoBookmark() {
        List<IGotoBookmark> handlers = extensionManager.getEnabledGotoBookmarkHandlers();
        return new GotoBookmark(handlers);
    }
}
