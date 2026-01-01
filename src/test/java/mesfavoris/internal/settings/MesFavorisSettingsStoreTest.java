package mesfavoris.internal.settings;

import org.jdom.Element;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MesFavorisSettingsStoreTest {

    @Test
    public void testDefaultSettings() {
        // Given
        MesFavorisSettingsStore store = new MesFavorisSettingsStore();

        // Then
        assertThat(store.isReplaceIntellijShortcuts()).isFalse();
    }

    @Test
    public void testSetReplaceIntellijShortcuts() {
        // Given
        MesFavorisSettingsStore store = new MesFavorisSettingsStore();

        // When
        store.setReplaceIntellijShortcuts(true);

        // Then
        assertThat(store.isReplaceIntellijShortcuts()).isTrue();
    }

    @Test
    public void testPersistenceGetState() {
        // Given
        MesFavorisSettingsStore store = new MesFavorisSettingsStore();
        store.setReplaceIntellijShortcuts(true);

        // When
        Element state = store.getState();

        // Then
        assertThat(state).isNotNull();
        assertThat(state.getName()).isEqualTo("MesFavorisSettings");
        assertThat(state.getAttributeValue("replaceIntellijShortcuts")).isEqualTo("true");
    }

    @Test
    public void testPersistenceLoadState() {
        // Given
        MesFavorisSettingsStore store = new MesFavorisSettingsStore();
        Element state = new Element("MesFavorisSettings");
        state.setAttribute("replaceIntellijShortcuts", "true");

        // When
        store.loadState(state);

        // Then
        assertThat(store.isReplaceIntellijShortcuts()).isTrue();
    }

    @Test
    public void testPersistenceLoadStateWithFalse() {
        // Given
        MesFavorisSettingsStore store = new MesFavorisSettingsStore();
        store.setReplaceIntellijShortcuts(true);
        Element state = new Element("MesFavorisSettings");
        state.setAttribute("replaceIntellijShortcuts", "false");

        // When
        store.loadState(state);

        // Then
        assertThat(store.isReplaceIntellijShortcuts()).isFalse();
    }

    @Test
    public void testPersistenceLoadStateWithMissingAttribute() {
        // Given
        MesFavorisSettingsStore store = new MesFavorisSettingsStore();
        store.setReplaceIntellijShortcuts(true);
        Element state = new Element("MesFavorisSettings");

        // When
        store.loadState(state);

        // Then - should keep previous value when attribute is missing
        assertThat(store.isReplaceIntellijShortcuts()).isTrue();
    }
}

