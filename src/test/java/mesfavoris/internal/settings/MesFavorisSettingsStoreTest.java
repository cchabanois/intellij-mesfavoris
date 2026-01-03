package mesfavoris.internal.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.messages.MessageBusConnection;
import org.jdom.Element;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MesFavorisSettingsStoreTest extends BasePlatformTestCase {

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

    @Test
    public void testMessageBusNotificationWhenSettingChanges() {
        // Given
        MesFavorisSettingsStore store = MesFavorisSettingsStore.getInstance();
        // Reset to false first
        store.setReplaceIntellijShortcuts(false);

        final boolean[] listenerCalled = {false};
        final boolean[] expectedValue = {false};

        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
        try {
            connection.subscribe(MesFavorisSettingsListener.TOPIC, enabled -> {
                listenerCalled[0] = true;
                expectedValue[0] = enabled;
            });

            // When
            store.setReplaceIntellijShortcuts(true);

            // Then
            assertThat(listenerCalled[0]).isTrue();
            assertThat(expectedValue[0]).isTrue();
        } finally {
            connection.disconnect();
        }
    }

    @Test
    public void testMessageBusNotificationNotCalledWhenSettingDoesNotChange() {
        // Given
        MesFavorisSettingsStore store = MesFavorisSettingsStore.getInstance();
        store.setReplaceIntellijShortcuts(true);
        final int[] listenerCallCount = {0};

        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
        try {
            connection.subscribe(MesFavorisSettingsListener.TOPIC, enabled -> listenerCallCount[0]++);

            // When
            store.setReplaceIntellijShortcuts(true);

            // Then
            assertThat(listenerCallCount[0]).isEqualTo(0);
        } finally {
            connection.disconnect();
        }
    }
}

