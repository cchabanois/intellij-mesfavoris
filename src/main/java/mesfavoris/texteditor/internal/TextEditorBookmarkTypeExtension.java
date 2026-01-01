package mesfavoris.texteditor.internal;

import com.intellij.icons.AllIcons;
import mesfavoris.extensions.AbstractBookmarkTypeExtension;
import mesfavoris.internal.placeholders.PathPlaceholderResolver;
import mesfavoris.internal.settings.placeholders.PathPlaceholdersStore;

import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.BookmarkPropertyType.*;
import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.bookmarkPropertyDescriptor;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.*;

/**
 * Bookmark type extension for text editor bookmarks.
 * Handles bookmarks that point to specific locations in text files.
 */
public class TextEditorBookmarkTypeExtension extends AbstractBookmarkTypeExtension {

    public static final String BOOKMARK_TYPE_NAME = "textEditor";

    public TextEditorBookmarkTypeExtension() {
        super(BOOKMARK_TYPE_NAME, "Bookmarks for text files with line and content information", AllIcons.FileTypes.Text);

        // Define properties for text editor bookmarks
        addProperty(bookmarkPropertyDescriptor(PROP_FILE_PATH)
                .type(PATH)
                .updatable(false)
                .description("File path")
                .build());

        addProperty(bookmarkPropertyDescriptor(PROP_LINE_NUMBER)
                .type(INT)
                .updatable(true)
                .description("Line number")
                .build());

        addProperty(bookmarkPropertyDescriptor(PROP_LINE_CONTENT)
                .type(STRING)
                .updatable(false)
                .description("Line content")
                .build());

        addProperty(bookmarkPropertyDescriptor(PROP_WORKSPACE_PATH)
                .type(PATH)
                .updatable(false)
                .description("Workspace relative path")
                .build());

        PathPlaceholdersStore placeholdersStore = PathPlaceholdersStore.getInstance();
        PathPlaceholderResolver pathPlaceholderResolver = new PathPlaceholderResolver(placeholdersStore);
        addLabelProvider(new TextEditorBookmarkLabelProvider(pathPlaceholderResolver));
        addPropertiesProvider(new TextEditorBookmarkPropertiesProvider(pathPlaceholderResolver), 100);
        addLocationProvider(new WorkspaceFileBookmarkLocationProvider(), 100);
        addLocationProvider(new ExternalFileBookmarkLocationProvider(pathPlaceholderResolver), 100);
        addGotoBookmarkHandler(new GotoWorkspaceFileBookmark(), 100);
        addGotoBookmarkHandler(new GotoExternalFileBookmark(), 100);
    }
}
