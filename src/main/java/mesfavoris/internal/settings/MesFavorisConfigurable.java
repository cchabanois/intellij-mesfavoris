package mesfavoris.internal.settings;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Main "Mes Favoris" configuration page in settings
 */
public class MesFavorisConfigurable implements Configurable {

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Mes Favoris";
    }

    @Override
    public @Nullable JComponent createComponent() {
        // Simple main page with information text
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("<html><h2>Mes Favoris Configuration</h2></html>");
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);

        panel.add(Box.createVerticalStrut(10));

        JLabel descriptionLabel = new JLabel("<html><body style='width: 500px'>" +
            "Mes Favoris allows you to manage your bookmarks in IntelliJ IDEA. " +
            "Use the sub-sections to configure different aspects of the plugin:" +
            "<ul>" +
            "<li><b>Placeholders</b>: Define shortcuts for frequently used paths</li>" +
            "<li><b>Bookmark Types</b>: Enable or disable specific bookmark types</li>" +
            "<li><b>Google Drive</b>: Configure OAuth credentials for Google Drive integration</li>" +
            "</ul>" +
            "</body></html>");
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(descriptionLabel);

        return panel;
    }

    @Override
    public boolean isModified() {
        // This main page has no modifiable settings
        return false;
    }

    @Override
    public void apply() {
        // Nothing to apply on this main page
    }

    @Override
    public void reset() {
        // Nothing to reset on this main page
    }

    @Override
    public void disposeUIResources() {
        // Nothing to clean up
    }
}
