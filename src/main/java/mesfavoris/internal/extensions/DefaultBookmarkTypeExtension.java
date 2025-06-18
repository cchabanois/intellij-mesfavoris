package mesfavoris.internal.extensions;

import mesfavoris.extensions.AbstractBookmarkTypeExtension;
import mesfavoris.model.Bookmark;
import mesfavoris.ui.renderers.BookmarkFolderLabelProvider;

import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.BookmarkPropertyType.INSTANT;
import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.BookmarkPropertyType.STRING;
import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.bookmarkPropertyDescriptor;

public class DefaultBookmarkTypeExtension extends AbstractBookmarkTypeExtension {

    public static final String BOOKMARK_TYPE_NAME = "default";

    public DefaultBookmarkTypeExtension() {
        super(BOOKMARK_TYPE_NAME, null);

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

    }
}
