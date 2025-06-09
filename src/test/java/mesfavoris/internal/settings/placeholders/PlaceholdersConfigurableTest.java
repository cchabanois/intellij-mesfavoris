package mesfavoris.internal.settings.placeholders;

import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.*;

/**
 * Test for PlaceholdersConfigurable
 */
public class PlaceholdersConfigurableTest {

    @Test
    public void testGetDisplayName() {
        // Given
        PlaceholdersConfigurable configurable = new PlaceholdersConfigurable();

        // When
        String displayName = configurable.getDisplayName();

        // Then
        assertEquals("Placeholders", displayName);
    }

    @Test
    public void testCreateComponent() {
        // Given
        PlaceholdersConfigurable configurable = new PlaceholdersConfigurable();

        // When
        JComponent component = configurable.createComponent();

        // Then
        assertNotNull("Component should not be null", component);
        assertTrue("Component should be visible", component.isVisible());
    }

    @Test
    public void testIsModifiedInitially() {
        // Given
        PlaceholdersConfigurable configurable = new PlaceholdersConfigurable();

        // When
        boolean isModified = configurable.isModified();

        // Then
        assertFalse("Should not be modified initially", isModified);
    }



    @Test
    public void testDisposeUIResources() {
        // Given
        PlaceholdersConfigurable configurable = new PlaceholdersConfigurable();
        configurable.createComponent(); // Initialize the component

        // When
        configurable.disposeUIResources();

        // Then
        // Should not throw any exception
        // Component should be recreated on next call
        JComponent newComponent = configurable.createComponent();
        assertNotNull("Component should be recreated after disposal", newComponent);
    }
}
