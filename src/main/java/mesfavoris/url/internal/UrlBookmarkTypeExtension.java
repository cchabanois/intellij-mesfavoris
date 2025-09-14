package mesfavoris.url.internal;

import com.intellij.icons.AllIcons;
import mesfavoris.extensions.AbstractBookmarkTypeExtension;
import mesfavoris.url.UrlBookmarkProperties;

import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.BookmarkPropertyType.STRING;
import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.bookmarkPropertyDescriptor;

/**
 * Bookmark type extension for URL bookmarks.
 * Handles bookmarks that point to web URLs.
 */
public class UrlBookmarkTypeExtension extends AbstractBookmarkTypeExtension {

    public static final String BOOKMARK_TYPE_NAME = "url";

    public UrlBookmarkTypeExtension() {
        super(BOOKMARK_TYPE_NAME, AllIcons.Nodes.PpWeb);

        // Define properties for URL bookmarks
        addProperty(bookmarkPropertyDescriptor(UrlBookmarkProperties.PROP_URL)
                .type(STRING)
                .description("URL")
                .updatable(true)
                .build());

        addProperty(bookmarkPropertyDescriptor("favicon")
                .type(STRING)
                .updatable(true)
                .description("icon")
                .build());

        addLabelProvider(new UrlBookmarkLabelProvider());
        addPropertiesProvider(new UrlBookmarkPropertiesProvider(), 10);
        addLocationProvider(new UrlBookmarkLocationProvider(), 10);
        addGotoBookmarkHandler(new GotoUrlBookmark(), 10);
    }
}
