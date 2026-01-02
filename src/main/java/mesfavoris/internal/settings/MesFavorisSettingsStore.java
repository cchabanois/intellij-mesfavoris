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
    
    private static final String ATTR_REPLACE_INTELLIJ_SHORTCUTS = "replaceIntellijShortcuts";
    
    private boolean replaceIntellijShortcuts = false;

    public static MesFavorisSettingsStore getInstance() {
        return ApplicationManager.getApplication().getService(MesFavorisSettingsStore.class);
    }

    @Override
    public @Nullable Element getState() {
        Element container = new Element("MesFavorisSettings");
        container.setAttribute(ATTR_REPLACE_INTELLIJ_SHORTCUTS, String.valueOf(replaceIntellijShortcuts));
        return container;
    }

    @Override
    public void loadState(@NotNull Element state) {
        String value = state.getAttributeValue(ATTR_REPLACE_INTELLIJ_SHORTCUTS);
        if (value != null) {
            replaceIntellijShortcuts = Boolean.parseBoolean(value);
        }
    }

    public boolean isReplaceIntellijShortcuts() {
        return replaceIntellijShortcuts;
    }

    public void setReplaceIntellijShortcuts(boolean replaceIntellijShortcuts) {
        this.replaceIntellijShortcuts = replaceIntellijShortcuts;
    }
}

