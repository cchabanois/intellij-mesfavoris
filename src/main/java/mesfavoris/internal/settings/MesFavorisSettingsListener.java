package mesfavoris.internal.settings;

import com.intellij.util.messages.Topic;

/**
 * Listener for Mesfavoris settings changes.
 * Events are published via MessageBus.
 */
public interface MesFavorisSettingsListener {
    Topic<MesFavorisSettingsListener> TOPIC = Topic.create("MesFavorisSettingsListener", MesFavorisSettingsListener.class);

    /**
     * Called when the "use IntelliJ bookmark shortcuts" setting changes
     * @param enabled true if IntelliJ shortcuts should be used, false to use Mesfavoris shortcuts
     */
    void useIntellijBookmarkShortcutsChanged(boolean enabled);
}

