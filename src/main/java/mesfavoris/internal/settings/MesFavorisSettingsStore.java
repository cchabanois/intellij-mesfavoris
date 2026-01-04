package mesfavoris.internal.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent store for general Mesfavoris settings
 */
@State(
    name = "MesFavorisSettings",
    storages = @Storage("mesfavoris.xml")
)
public class MesFavorisSettingsStore implements PersistentStateComponent<Element> {

    private static final String ATTR_USE_INTELLIJ_BOOKMARK_SHORTCUTS = "useIntellijBookmarkShortcuts";

    private boolean useIntellijBookmarkShortcuts = false;

    public static MesFavorisSettingsStore getInstance() {
        return ApplicationManager.getApplication().getService(MesFavorisSettingsStore.class);
    }

    @Override
    public @Nullable Element getState() {
        Element container = new Element("MesFavorisSettings");
        container.setAttribute(ATTR_USE_INTELLIJ_BOOKMARK_SHORTCUTS, String.valueOf(useIntellijBookmarkShortcuts));
        return container;
    }

    @Override
    public void loadState(@NotNull Element state) {
        String value = state.getAttributeValue(ATTR_USE_INTELLIJ_BOOKMARK_SHORTCUTS);
        if (value != null) {
            useIntellijBookmarkShortcuts = Boolean.parseBoolean(value);
        }
    }

    public boolean isUseIntellijBookmarkShortcuts() {
        return useIntellijBookmarkShortcuts;
    }

    public void setUseIntellijBookmarkShortcuts(boolean useIntellijBookmarkShortcuts) {
        if (this.useIntellijBookmarkShortcuts != useIntellijBookmarkShortcuts) {
            this.useIntellijBookmarkShortcuts = useIntellijBookmarkShortcuts;
            fireUseIntellijBookmarkShortcutsChanged(useIntellijBookmarkShortcuts);
        }
    }

    private void fireUseIntellijBookmarkShortcutsChanged(boolean enabled) {
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(MesFavorisSettingsListener.TOPIC)
                .useIntellijBookmarkShortcutsChanged(enabled);
    }
}

