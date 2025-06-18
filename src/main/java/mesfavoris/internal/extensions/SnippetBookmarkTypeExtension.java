package mesfavoris.internal.extensions;

import com.intellij.icons.AllIcons;
import mesfavoris.extensions.AbstractBookmarkTypeExtension;
import mesfavoris.internal.snippets.*;

import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.BookmarkPropertyType.STRING;
import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.bookmarkPropertyDescriptor;

/**
 * Bookmark type extension for snippet bookmarks.
 * Handles bookmarks that contain code snippets.
 */
public class SnippetBookmarkTypeExtension extends AbstractBookmarkTypeExtension {

    public static final String BOOKMARK_TYPE_NAME = "snippet";

    public SnippetBookmarkTypeExtension() {
        super(BOOKMARK_TYPE_NAME, AllIcons.FileTypes.Text);

        // Define properties for snippet bookmarks
        addProperty(bookmarkPropertyDescriptor(SnippetBookmarkProperties.PROP_SNIPPET_CONTENT)
                .type(STRING)
                .updatable(false)
                .description("Snippet content")
                .build());

        addLabelProvider(new SnippetBookmarkLabelProvider());
        addPropertiesProvider(new SnippetBookmarkPropertiesProvider(), 80);
        addLocationProvider(new SnippetBookmarkLocationProvider(), 80);
        addGotoBookmarkHandler(new GotoSnippetBookmark(), 80);
    }
}
