package mesfavoris.internal.settings;

import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Main "Mes Favoris" configuration page in settings
 */
public class MesFavorisConfigurable implements Configurable {

    private JBCheckBox useIntellijBookmarkShortcutsCheckbox;
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

        String checkboxText = String.format(
                "Use IntelliJ IDEA bookmark shortcuts (%s) instead of Mesfavoris shortcuts (%s)",
                getShortcutsText("ShowBookmarks"),
                getShortcutsText("mesfavoris.actions.ShowBookmarksAction")
        );
        useIntellijBookmarkShortcutsCheckbox = new JBCheckBox(checkboxText);
        useIntellijBookmarkShortcutsCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(useIntellijBookmarkShortcutsCheckbox);

        JLabel warningLabel = new JLabel("<html><body style='width: 500px; color: gray;'>" +
            "<i>When checked, Mesfavoris actions will replace IntelliJ's bookmark actions.</i>" +
            "</body></html>");
        warningLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        warningLabel.setBorder(JBUI.Borders.emptyLeft(20));
        panel.add(warningLabel);

        return panel;
    }

    private String getShortcutsText(String actionId) {
        Keymap activeKeymap = KeymapManager.getInstance().getActiveKeymap();
        Shortcut[] shortcuts = activeKeymap.getShortcuts(actionId);

        if (shortcuts.length == 0) {
            return "no shortcut";
        }

        return Arrays.stream(shortcuts)
            .map(KeymapUtil::getShortcutText)
            .collect(Collectors.joining(", "));
    }

    @Override
    public boolean isModified() {
        if (useIntellijBookmarkShortcutsCheckbox == null) {
            return false;
        }
        return useIntellijBookmarkShortcutsCheckbox.isSelected() != settingsStore.isUseIntellijBookmarkShortcuts();
    }

    @Override
    public void apply() {
        if (useIntellijBookmarkShortcutsCheckbox != null) {
            settingsStore.setUseIntellijBookmarkShortcuts(useIntellijBookmarkShortcutsCheckbox.isSelected());
        }
    }

    @Override
    public void reset() {
        if (useIntellijBookmarkShortcutsCheckbox != null) {
            useIntellijBookmarkShortcutsCheckbox.setSelected(settingsStore.isUseIntellijBookmarkShortcuts());
        }
    }

    @Override
    public void disposeUIResources() {
        useIntellijBookmarkShortcutsCheckbox = null;
        settingsStore = null;
    }
}
