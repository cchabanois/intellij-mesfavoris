package mesfavoris.java.internal;

import com.intellij.icons.AllIcons;
import mesfavoris.extensions.AbstractBookmarkTypeExtension;
import mesfavoris.java.internal.editor.JavaEditorBookmarkPropertiesProvider;

import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.BookmarkPropertyType.INT;
import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.BookmarkPropertyType.STRING;
import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.bookmarkPropertyDescriptor;
import static mesfavoris.java.JavaBookmarkProperties.*;

/**
 * Bookmark type extension for Java files
 */
public class JavaBookmarkTypeExtension extends AbstractBookmarkTypeExtension {

    public static final String BOOKMARK_TYPE_NAME = "java";

    public JavaBookmarkTypeExtension() {
        super(BOOKMARK_TYPE_NAME, "Bookmarks for Java code elements like classes, methods, and fields", AllIcons.FileTypes.Java);

        // Define properties for Java bookmarks
        addProperty(bookmarkPropertyDescriptor(PROP_JAVA_ELEMENT_NAME)
                .type(STRING)
                .updatable(true)
                .description("Java element name")
                .build());

        addProperty(bookmarkPropertyDescriptor(PROP_JAVA_DECLARING_TYPE)
                .type(STRING)
                .updatable(true)
                .description("Declaring type")
                .build());

        addProperty(bookmarkPropertyDescriptor(PROP_JAVA_ELEMENT_KIND)
                .type(STRING)
                .updatable(true)
                .description("Element kind")
                .build());

        addProperty(bookmarkPropertyDescriptor(PROP_JAVA_METHOD_SIGNATURE)
                .type(STRING)
                .updatable(true)
                .description("Method signature")
                .build());

        addProperty(bookmarkPropertyDescriptor(PROP_JAVA_TYPE)
                .type(STRING)
                .updatable(true)
                .description("Java type")
                .build());

        addProperty(bookmarkPropertyDescriptor(PROP_LINE_NUMBER_INSIDE_ELEMENT)
                .type(INT)
                .updatable(true)
                .description("Line number inside element")
                .build());

        // Add providers
        addPropertiesProvider(new JavaEditorBookmarkPropertiesProvider(), 20);
        addLocationProvider(new JavaTypeMemberBookmarkLocationProvider(), 20);
        addGotoBookmarkHandler(new GotoInsideJavaElementBookmark(), 20);
    }
}
