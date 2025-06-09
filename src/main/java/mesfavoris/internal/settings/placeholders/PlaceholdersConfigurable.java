package mesfavoris.internal.settings.placeholders;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import mesfavoris.placeholders.PathPlaceholder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Placeholders configuration page in settings
 */
public class PlaceholdersConfigurable implements Configurable {
    
    private PlaceholdersTablePanel placeholdersPanel;
    private List<PathPlaceholder> originalPlaceholders;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Placeholders";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (placeholdersPanel == null) {
            placeholdersPanel = new PlaceholdersTablePanel();
            // Initialize with empty data first
            placeholdersPanel.setPlaceholders(new ArrayList<>());
            reset(); // Load actual data
        }
        return placeholdersPanel;
    }

    @Override
    public boolean isModified() {
        if (placeholdersPanel == null || originalPlaceholders == null) {
            return false;
        }
        return placeholdersPanel.isModified(originalPlaceholders);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (placeholdersPanel == null) {
            return;
        }

        List<PathPlaceholder> placeholders = placeholdersPanel.getPlaceholders();

        // Data validation
        validatePlaceholders(placeholders);

        // Save
        PathPlaceholdersStore store = PathPlaceholdersStore.getInstance();
        store.setPlaceholders(placeholders);

        // Update original data
        originalPlaceholders = new ArrayList<>(placeholders);
    }

    @Override
    public void reset() {
        if (placeholdersPanel == null) {
            return;
        }

        try {
            PathPlaceholdersStore store = PathPlaceholdersStore.getInstance();
            originalPlaceholders = store.getPlaceholders();

            // Create a copy to avoid direct modifications
            List<PathPlaceholder> copy = new ArrayList<>(originalPlaceholders);

            placeholdersPanel.setPlaceholders(copy);
        } catch (Exception e) {
            // Initialize with empty list if there's an error
            originalPlaceholders = new ArrayList<>();
            placeholdersPanel.setPlaceholders(new ArrayList<>());
        }
    }

    @Override
    public void disposeUIResources() {
        placeholdersPanel = null;
        originalPlaceholders = null;
    }

    private void validatePlaceholders(List<PathPlaceholder> placeholders) throws ConfigurationException {
        Set<String> names = new HashSet<>();

        for (PathPlaceholder placeholder : placeholders) {
            String name = placeholder.getName();

            // Check name uniqueness
            if (names.contains(name)) {
                throw new ConfigurationException("Placeholder name '" + name + "' is used multiple times");
            }
            names.add(name);

            // Check that name does not contain forbidden characters
            if (name.contains("${") || name.contains("}")) {
                throw new ConfigurationException("Placeholder name '" + name + "' cannot contain '${' or '}'");
            }
        }
    }
}
