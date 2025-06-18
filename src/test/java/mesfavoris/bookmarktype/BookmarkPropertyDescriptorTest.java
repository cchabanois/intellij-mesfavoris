package mesfavoris.bookmarktype;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import static mesfavoris.bookmarktype.BookmarkPropertyDescriptor.bookmarkPropertyDescriptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for BookmarkPropertyDescriptor and its builder
 */
public class BookmarkPropertyDescriptorTest extends BasePlatformTestCase {

    public void testBookmarkPropertyDescriptorBuilder() {
        // Given & When - Create property descriptor using builder pattern
        BookmarkPropertyDescriptor descriptor = bookmarkPropertyDescriptor("testProperty")
                .type(BookmarkPropertyDescriptor.BookmarkPropertyType.PATH)
                .updatable(false)
                .description("Test description")
                .build();

        // Then
        assertThat(descriptor)
                .isNotNull()
                .satisfies(d -> {
                    assertThat(d.getName()).isEqualTo("testProperty");
                    assertThat(d.getType()).isEqualTo(BookmarkPropertyDescriptor.BookmarkPropertyType.PATH);
                    assertThat(d.isUpdatable()).isFalse();
                    assertThat(d.getDescription()).isEqualTo("Test description");
                });
    }

    public void testBookmarkPropertyDescriptorBuilderDefaults() {
        // Given & When - Create property descriptor with minimal builder usage
        BookmarkPropertyDescriptor descriptor = bookmarkPropertyDescriptor("simpleProperty")
                .build();

        // Then - Verify default values
        assertThat(descriptor)
                .isNotNull()
                .satisfies(d -> {
                    assertThat(d.getName()).isEqualTo("simpleProperty");
                    assertThat(d.getType()).isEqualTo(BookmarkPropertyDescriptor.BookmarkPropertyType.STRING); // Default
                    assertThat(d.isUpdatable()).isTrue(); // Default
                    assertThat(d.getDescription()).isNull(); // Default
                });
    }

    public void testBookmarkPropertyDescriptorBuilderValidation() {
        // When & Then - Test validation
        assertThatThrownBy(() -> bookmarkPropertyDescriptor(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Property name cannot be null or empty");

        assertThatThrownBy(() -> bookmarkPropertyDescriptor(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Property name cannot be null or empty");

        assertThatThrownBy(() -> bookmarkPropertyDescriptor("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Property name cannot be null or empty");
    }

    public void testBookmarkPropertyDescriptorEquality() {
        // Given
        BookmarkPropertyDescriptor descriptor1 = bookmarkPropertyDescriptor("testProp")
                .type(BookmarkPropertyDescriptor.BookmarkPropertyType.STRING)
                .updatable(true)
                .description("Test")
                .build();

        BookmarkPropertyDescriptor descriptor2 = bookmarkPropertyDescriptor("testProp")
                .type(BookmarkPropertyDescriptor.BookmarkPropertyType.STRING)
                .updatable(true)
                .description("Test")
                .build();

        BookmarkPropertyDescriptor descriptor3 = bookmarkPropertyDescriptor("differentProp")
                .type(BookmarkPropertyDescriptor.BookmarkPropertyType.STRING)
                .updatable(true)
                .description("Test")
                .build();

        // Then
        assertThat(descriptor1).isEqualTo(descriptor2);
        assertThat(descriptor1).isNotEqualTo(descriptor3);
        assertThat(descriptor1.hashCode()).isEqualTo(descriptor2.hashCode());
    }

    public void testBookmarkPropertyDescriptorTypes() {
        // Test all property types
        for (BookmarkPropertyDescriptor.BookmarkPropertyType type : BookmarkPropertyDescriptor.BookmarkPropertyType.values()) {
            BookmarkPropertyDescriptor descriptor = bookmarkPropertyDescriptor("test_" + type.name())
                    .type(type)
                    .build();

            assertThat(descriptor.getType()).isEqualTo(type);
        }
    }

    public void testBookmarkPropertyDescriptorToString() {
        // Given
        BookmarkPropertyDescriptor descriptor = bookmarkPropertyDescriptor("testProperty")
                .type(BookmarkPropertyDescriptor.BookmarkPropertyType.PATH)
                .updatable(false)
                .description("Test description")
                .build();

        // When
        String toString = descriptor.toString();

        // Then - toString() returns only the property name
        assertThat(toString).isEqualTo("testProperty");
    }
}
