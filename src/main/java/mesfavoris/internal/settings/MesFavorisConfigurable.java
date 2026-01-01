package mesfavoris.internal.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Main "Mes Favoris" configuration page in settings
 */
public class MesFavorisConfigurable implements Configurable {

    private JBCheckBox replaceIntellijShortcutsCheckbox;
    private MesFavorisSettingsStore settingsStore;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Mes Favoris";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsStore = MesFavorisSettingsStore.getInstance();

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

        panel.add(Box.createVerticalStrut(20));

        // Keyboard shortcuts section
        JLabel shortcutsLabel = new JLabel("<html><h3>Keyboard Shortcuts</h3></html>");
        shortcutsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(shortcutsLabel);

        panel.add(Box.createVerticalStrut(5));

        replaceIntellijShortcutsCheckbox = new JBCheckBox(
            "Replace IntelliJ IDEA bookmark shortcuts with Mesfavoris shortcuts"
        );
        replaceIntellijShortcutsCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(replaceIntellijShortcutsCheckbox);

        JLabel warningLabel = new JLabel("<html><body style='width: 500px; color: gray;'>" +
            "<i>When enabled, F11 and Shift+F11 will use Mesfavoris instead of IntelliJ's built-in bookmarks. " +
            "Restart IntelliJ IDEA for changes to take effect.</i>" +
            "</body></html>");
        warningLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        warningLabel.setBorder(JBUI.Borders.emptyLeft(20));
        panel.add(warningLabel);

        return panel;
    }

    @Override
    public boolean isModified() {
        if (replaceIntellijShortcutsCheckbox == null) {
            return false;
        }
        return replaceIntellijShortcutsCheckbox.isSelected() != settingsStore.isReplaceIntellijShortcuts();
    }

    @Override
    public void apply() {
        if (replaceIntellijShortcutsCheckbox != null) {
            settingsStore.setReplaceIntellijShortcuts(replaceIntellijShortcutsCheckbox.isSelected());
        }
    }

    @Override
    public void reset() {
        if (replaceIntellijShortcutsCheckbox != null) {
            replaceIntellijShortcutsCheckbox.setSelected(settingsStore.isReplaceIntellijShortcuts());
        }
    }

    @Override
    public void disposeUIResources() {
        replaceIntellijShortcutsCheckbox = null;
        settingsStore = null;
    }
}
