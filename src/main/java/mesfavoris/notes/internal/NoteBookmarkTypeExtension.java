package mesfavoris.notes.internal;

import mesfavoris.extensions.AbstractBookmarkTypeExtension;
import mesfavoris.notes.NoteBookmarkProperties;
import mesfavoris.notes.ui.NoteBookmarkDetailsPart;

import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.BookmarkPropertyType.STRING;
import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.bookmarkPropertyDescriptor;
public class NoteBookmarkTypeExtension extends AbstractBookmarkTypeExtension {
    public static final String TYPE = "note";

    public NoteBookmarkTypeExtension() {
        super(TYPE, "Note bookmark type", null);

        // Define properties for note bookmarks
        addProperty(bookmarkPropertyDescriptor(NoteBookmarkProperties.PROP_NOTES)
                .type(STRING)
                .updatable(false)
                .build());

        // Add label provider
        addLabelProvider(new NoteBookmarkLabelProvider());
        
        // Add detail part providers
        addDetailPartProvider(NoteBookmarkDetailsPart::new, 0);
    }
}
