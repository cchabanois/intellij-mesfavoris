package mesfavoris.internal.bookmarktypes.extension;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.bookmarktype.BookmarkPropertyDescriptor;
import mesfavoris.internal.settings.bookmarktypes.BookmarkTypesStore;
import mesfavoris.model.Bookmark;

import java.util.List;

import static mesfavoris.texteditor.TextEditorBookmarkProperties.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ExtensionBookmarkPropertyDescriptorsTest extends BasePlatformTestCase {

    private ExtensionBookmarkPropertyDescriptors propertyDescriptors;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        propertyDescriptors = new ExtensionBookmarkPropertyDescriptors();
    }

    public void testGetPropertyDescriptors() {
        // When
        List<BookmarkPropertyDescriptor> descriptors = propertyDescriptors.getPropertyDescriptors();

        // Then
        assertThat(descriptors)
                .isNotNull()
                .isNotEmpty();

        // Verify that we have descriptors from different bookmark types
        List<String> propertyNames = descriptors.stream()
                .map(BookmarkPropertyDescriptor::getName)
                .toList();

        // Should have properties from default bookmark type
        assertThat(propertyNames).contains(
                Bookmark.PROPERTY_NAME,
                Bookmark.PROPERTY_COMMENT,
                Bookmark.PROPERTY_CREATED
        );

        // Should have properties from textEditor bookmark type
        assertThat(propertyNames).contains(
                PROP_FILE_PATH,
                PROP_LINE_NUMBER,
                PROP_LINE_CONTENT
        );
    }

    public void testGetPropertyDescriptor() {
        // When
        BookmarkPropertyDescriptor filePathDescriptor = propertyDescriptors.getPropertyDescriptor(PROP_FILE_PATH);
        BookmarkPropertyDescriptor nameDescriptor = propertyDescriptors.getPropertyDescriptor(Bookmark.PROPERTY_NAME);

        // Then
        assertThat(filePathDescriptor).isNotNull();
        assertThat(filePathDescriptor.getName()).isEqualTo(PROP_FILE_PATH);
        assertThat(filePathDescriptor.getType()).isEqualTo(BookmarkPropertyDescriptor.BookmarkPropertyType.PATH);

        assertThat(nameDescriptor).isNotNull();
        assertThat(nameDescriptor.getName()).isEqualTo(Bookmark.PROPERTY_NAME);
        assertThat(nameDescriptor.getType()).isEqualTo(BookmarkPropertyDescriptor.BookmarkPropertyType.STRING);
    }

    public void testGetPropertyDescriptorForNonExistentProperty() {
        // When
        BookmarkPropertyDescriptor descriptor = propertyDescriptors.getPropertyDescriptor("nonExistentProperty");

        // Then
        assertThat(descriptor).isNull();
    }

    public void testGetPropertyDescriptorsFiltersDisabledTypes() {
        // Given
        BookmarkTypesStore store = BookmarkTypesStore.getInstance();

        // Get initial descriptors
        List<BookmarkPropertyDescriptor> initialDescriptors = propertyDescriptors.getPropertyDescriptors();
        int initialCount = initialDescriptors.size();

        // Disable the "textEditor" bookmark type
        store.setDisabledBookmarkTypes(java.util.Set.of("textEditor"));

        // When
        List<BookmarkPropertyDescriptor> filteredDescriptors = propertyDescriptors.getPropertyDescriptors();

        // Then
        assertThat(filteredDescriptors)
                .isNotNull()
                .hasSizeLessThan(initialCount);

        // Verify that textEditor properties are not included
        List<String> propertyNames = filteredDescriptors.stream()
                .map(BookmarkPropertyDescriptor::getName)
                .toList();

        assertThat(propertyNames)
                .doesNotContain(PROP_FILE_PATH, PROP_LINE_NUMBER, PROP_LINE_CONTENT);

        // But should still contain default properties
        assertThat(propertyNames)
                .contains(Bookmark.PROPERTY_NAME, Bookmark.PROPERTY_COMMENT);

        // Clean up - re-enable all types
        store.setDisabledBookmarkTypes(java.util.Set.of());

        // Verify descriptors are back
        List<BookmarkPropertyDescriptor> restoredDescriptors = propertyDescriptors.getPropertyDescriptors();
        assertThat(restoredDescriptors).hasSizeGreaterThanOrEqualTo(initialCount);
    }

    public void testGetPropertyDescriptorReturnsNullForDisabledType() {
        // Given
        BookmarkTypesStore store = BookmarkTypesStore.getInstance();

        // Disable the "textEditor" bookmark type
        store.setDisabledBookmarkTypes(java.util.Set.of("textEditor"));

        // When
        BookmarkPropertyDescriptor descriptor = propertyDescriptors.getPropertyDescriptor(PROP_FILE_PATH);

        // Then
        assertThat(descriptor).isNull();

        // Clean up - re-enable all types
        store.setDisabledBookmarkTypes(java.util.Set.of());
    }
}

