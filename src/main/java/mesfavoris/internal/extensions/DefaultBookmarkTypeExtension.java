package mesfavoris.internal.extensions;

import mesfavoris.extensions.AbstractBookmarkTypeExtension;
import mesfavoris.internal.ui.details.CommentBookmarkDetailPart;
import mesfavoris.internal.ui.details.TagsBookmarkDetailPart;
import mesfavoris.internal.ui.details.BookmarkPropertiesDetailPart;
import mesfavoris.internal.ui.details.MarkerBookmarkDetailPart;
import mesfavoris.internal.mcp.McpBookmarkProperties;
import mesfavoris.model.Bookmark;
import mesfavoris.tags.TagsBookmarkProperties;
import mesfavoris.ui.renderers.BookmarkFolderLabelProvider;

import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.BookmarkPropertyType.INSTANT;
import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.BookmarkPropertyType.STRING;
import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.bookmarkPropertyDescriptor;

public class DefaultBookmarkTypeExtension extends AbstractBookmarkTypeExtension {

    public static final String BOOKMARK_TYPE_NAME = "default";

    public DefaultBookmarkTypeExtension() {
        super(BOOKMARK_TYPE_NAME, "Default bookmark type with basic properties and functionality", null);

        // Define properties for snippet bookmarks
        addProperty(bookmarkPropertyDescriptor(Bookmark.PROPERTY_NAME)
                .type(STRING)
                .updatable(false)
                .build());

        addProperty(bookmarkPropertyDescriptor(Bookmark.PROPERTY_COMMENT)
                .type(STRING)
                .updatable(false)
                .description("Free-text comment for the bookmark. Only the first line is shown in the bookmark tree; the full multi-line comment is visible in the details panel.")
                .build());

        addProperty(bookmarkPropertyDescriptor(McpBookmarkProperties.PROPERTY_ORIGIN)
                .type(STRING)
                .updatable(false)
                .description("Source that created this bookmark. 'mcp' if created via MCP tools, empty if created by the user.")
                .build());

        addProperty(bookmarkPropertyDescriptor(Bookmark.PROPERTY_CREATED)
                .type(INSTANT)
                .updatable(false)
                .build());

        addProperty(bookmarkPropertyDescriptor("modified")
                .type(INSTANT)
                .updatable(false)
                .build());

        addProperty(bookmarkPropertyDescriptor(TagsBookmarkProperties.PROP_TAGS)
                .type(STRING)
                .updatable(true)
                .description("Comma-separated list of tags for the bookmark, e.g. \"bug,perf\". Tag names cannot contain commas.")
                .build());

        addLabelProvider(new BookmarkFolderLabelProvider());

        // Add detail part providers
        addDetailPartProvider(CommentBookmarkDetailPart::new);
        addDetailPartProvider(TagsBookmarkDetailPart::new);
        addDetailPartProvider(BookmarkPropertiesDetailPart::new);
        addDetailPartProvider(MarkerBookmarkDetailPart::new);

    }
}
