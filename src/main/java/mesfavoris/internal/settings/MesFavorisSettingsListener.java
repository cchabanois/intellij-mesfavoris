package mesfavoris.internal.settings;

import com.intellij.util.messages.Topic;

/**
 * Listener for Mesfavoris settings changes.
 * Events are published via MessageBus.
 */
public interface MesFavorisSettingsListener {
    Topic<MesFavorisSettingsListener> TOPIC = Topic.create("MesFavorisSettingsListener", MesFavorisSettingsListener.class);

    /**
     * Called when the "replace IntelliJ shortcuts" setting changes
     * @param enabled true if shortcuts should be replaced, false otherwise
     */
    void replaceIntellijShortcutsChanged(boolean enabled);
}

