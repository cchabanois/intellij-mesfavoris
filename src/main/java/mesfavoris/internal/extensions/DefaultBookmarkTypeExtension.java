package mesfavoris.internal.extensions;

import mesfavoris.extensions.AbstractBookmarkTypeExtension;
import mesfavoris.internal.ui.details.CommentBookmarkDetailPart;
import mesfavoris.internal.ui.details.BookmarkPropertiesDetailPart;
import mesfavoris.internal.ui.details.MarkerBookmarkDetailPart;
import mesfavoris.internal.ui.details.PreviewBookmarkDetailPart;
import mesfavoris.model.Bookmark;
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
                .build());

        addProperty(bookmarkPropertyDescriptor(Bookmark.PROPERTY_CREATED)
                .type(INSTANT)
                .updatable(false)
                .build());

        addProperty(bookmarkPropertyDescriptor("modified")
                .type(INSTANT)
                .updatable(false)
                .build());

        addLabelProvider(new BookmarkFolderLabelProvider());

        // Add detail part providers
        addDetailPartProvider(CommentBookmarkDetailPart::new);
        addDetailPartProvider(BookmarkPropertiesDetailPart::new);
        addDetailPartProvider(MarkerBookmarkDetailPart::new);
        addDetailPartProvider(PreviewBookmarkDetailPart::new);

    }
}
