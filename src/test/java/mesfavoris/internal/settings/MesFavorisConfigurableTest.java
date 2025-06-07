package mesfavoris.internal.settings;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test for MesFavorisConfigurable
 */
public class MesFavorisConfigurableTest {

    @Test
    public void testGetDisplayName() {
        // Given
        MesFavorisConfigurable configurable = new MesFavorisConfigurable();

        // When
        String displayName = configurable.getDisplayName();

        // Then
        assertEquals("Mes Favoris", displayName);
    }

    @Test
    public void testCreateComponent() {
        // Given
        MesFavorisConfigurable configurable = new MesFavorisConfigurable();

        // When
        var component = configurable.createComponent();

        // Then
        assertNotNull(component);
    }

    @Test
    public void testIsModified() {
        // Given
        MesFavorisConfigurable configurable = new MesFavorisConfigurable();

        // When
        boolean isModified = configurable.isModified();

        // Then
        assertFalse(isModified); // Main page has no modifiable settings
    }
}
