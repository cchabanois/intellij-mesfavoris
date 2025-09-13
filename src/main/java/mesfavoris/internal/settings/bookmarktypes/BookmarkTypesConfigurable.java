package mesfavoris.internal.settings.bookmarktypes;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Set;

/**
 * Configurable for managing bookmark types settings
 */
public class BookmarkTypesConfigurable implements Configurable {

    private BookmarkTypesPanel panel;
    private BookmarkTypesStore store;

    @Override
    public String getDisplayName() {
        return "Bookmark Types";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (panel == null) {
            store = BookmarkTypesStore.getInstance();
            panel = new BookmarkTypesPanel(store.getDisabledBookmarkTypes());
        }
        return panel;
    }

    @Override
    public boolean isModified() {
        Set<String> storeDisabledTypes = store.getDisabledBookmarkTypes();
        Set<String> currentDisabledTypes = panel.getDisabledBookmarkTypes();
        return !currentDisabledTypes.equals(storeDisabledTypes);
    }

    @Override
    public void apply() throws ConfigurationException {
        Set<String> disabledTypes = panel.getDisabledBookmarkTypes();
        store.setDisabledBookmarkTypes(disabledTypes);
    }

    @Override
    public void reset() {
        panel.setDisabledBookmarkTypes(store.getDisabledBookmarkTypes());
    }

    @Override
    public void disposeUIResources() {
        panel = null;
    }
}
