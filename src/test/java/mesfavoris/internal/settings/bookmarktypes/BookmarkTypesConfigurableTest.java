package mesfavoris.internal.settings.bookmarktypes;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for BookmarkTypesConfigurable
 */
public class BookmarkTypesConfigurableTest extends BasePlatformTestCase {

    @Test
    public void testGetDisplayName() {
        // Given
        BookmarkTypesConfigurable configurable = new BookmarkTypesConfigurable();

        // When
        String displayName = configurable.getDisplayName();

        // Then
        assertThat(displayName).isEqualTo("Bookmark Types");
    }

    @Test
    public void testIsModifiedInitially() {
        // Given
        BookmarkTypesConfigurable configurable = new BookmarkTypesConfigurable();
        configurable.createComponent(); // Initialize the component

        // When
        boolean isModified = configurable.isModified();

        // Then
        assertThat(isModified).isFalse();
    }

    @Test
    public void testIsModifiedAfterChanges() {
        // Given
        BookmarkTypesConfigurable configurable = new BookmarkTypesConfigurable();
        BookmarkTypesPanel panel = (BookmarkTypesPanel) configurable.createComponent();
        BookmarkTypesTableModel tableModel = panel.getTableModel();

        // When
        tableModel.setValueAt(false, 0, 0); // Disable first bookmark type

        // Then
        assertThat(configurable.isModified()).isTrue();
    }

    @Test
    public void testApplyAndReset() throws Exception {
        // Given
        BookmarkTypesConfigurable configurable = new BookmarkTypesConfigurable();
        BookmarkTypesPanel panel = (BookmarkTypesPanel) configurable.createComponent();
        BookmarkTypesTableModel tableModel = panel.getTableModel();
        tableModel.setValueAt(false, 0, 0); // Disable first bookmark type
        assertThat(configurable.isModified()).isTrue();

        // When - apply changes
        configurable.apply();

        // Then - should not be modified after apply
        assertThat(configurable.isModified()).isFalse();

        // Verify the store was updated
        BookmarkTypesStore store = BookmarkTypesStore.getInstance();
        Set<String> disabledTypes = store.getDisabledBookmarkTypes();
        assertThat(disabledTypes).isNotEmpty();

        // When - reset after modifying UI
        tableModel.setValueAt(true, 0, 0); // Re-enable first bookmark type
        assertThat(configurable.isModified()).isTrue(); // Should be modified now

        configurable.reset();

        // Then - should restore from store
        assertThat(configurable.isModified()).isFalse();
        Set<String> currentDisabledTypes = panel.getDisabledBookmarkTypes();
        assertThat(currentDisabledTypes).isEqualTo(disabledTypes);
    }

}
