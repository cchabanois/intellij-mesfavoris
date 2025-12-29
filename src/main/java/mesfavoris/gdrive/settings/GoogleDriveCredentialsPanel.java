package mesfavoris.gdrive.settings;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import mesfavoris.gdrive.connection.GoogleOAuthClientConfigStore;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

/**
 * Panel for configuring Google Drive OAuth credentials
 */
public class GoogleDriveCredentialsPanel extends JPanel {
    private final Project project;
    private JRadioButton useDefaultCredentialsRadio;
    private JRadioButton useCustomCredentialsRadio;
    private JBTextField customClientIdField;
    private JBPasswordField customClientSecretField;
    private JPanel customCredentialsPanel;

    public GoogleDriveCredentialsPanel(@NotNull Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(10));
        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("<html><h3>Google Drive OAuth Credentials</h3></html>");
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        ButtonGroup group = new ButtonGroup();
        
        useDefaultCredentialsRadio = new JRadioButton("Use default credentials (built-in)");
        useDefaultCredentialsRadio.setAlignmentX(Component.LEFT_ALIGNMENT);
        useDefaultCredentialsRadio.addActionListener(e -> updateCustomFieldsState());
        group.add(useDefaultCredentialsRadio);
        mainPanel.add(useDefaultCredentialsRadio);

        JBLabel warningLabel = new JBLabel(
                "<html><i>⚠ You will see a Google security warning because the app is not verified.</i></html>",
                UIUtil.ComponentStyle.SMALL,
                UIUtil.FontColor.BRIGHTER
        );
        warningLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        warningLabel.setBorder(JBUI.Borders.emptyLeft(25));
        mainPanel.add(warningLabel);
        mainPanel.add(Box.createVerticalStrut(15));

        useCustomCredentialsRadio = new JRadioButton("Use custom credentials");
        useCustomCredentialsRadio.setAlignmentX(Component.LEFT_ALIGNMENT);
        useCustomCredentialsRadio.addActionListener(e -> updateCustomFieldsState());
        group.add(useCustomCredentialsRadio);
        mainPanel.add(useCustomCredentialsRadio);

        customCredentialsPanel = createCustomCredentialsPanel();
        customCredentialsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        customCredentialsPanel.setBorder(JBUI.Borders.emptyLeft(25));
        mainPanel.add(customCredentialsPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        JPanel helpPanel = new JPanel();
        helpPanel.setLayout(new BoxLayout(helpPanel, BoxLayout.Y_AXIS));
        helpPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        helpPanel.setBorder(JBUI.Borders.emptyLeft(25));

        JBLabel helpTitle = new JBLabel(
                "<html><b>How to create your own Google OAuth credentials:</b></html>",
                UIUtil.ComponentStyle.SMALL,
                UIUtil.FontColor.NORMAL
        );
        helpTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        helpPanel.add(helpTitle);
        helpPanel.add(Box.createVerticalStrut(5));

        JEditorPane helpText = new JEditorPane();
        helpText.setContentType("text/html");
        helpText.setText(
                "<html><body style='width: 600px; font-family: " + UIUtil.getLabelFont().getFamily() + "; font-size: " + UIUtil.getLabelFont().getSize() + "pt;'>" +
                "<ol style='margin-left: 15px; margin-top: 0; margin-bottom: 0;'>" +
                "<li>Go to <a href='https://console.cloud.google.com/'>Google Cloud Console</a></li>" +
                "<li>Create a new project (or use an existing one)</li>" +
                "<li>Enable the <b>Google Drive API</b></li>" +
                "<li>Go to <b>APIs & Services → Credentials</b></li>" +
                "<li>Click <b>Create Credentials → OAuth 2.0 Client ID</b></li>" +
                "<li>Choose <b>Desktop app</b> as application type</li>" +
                "<li>Copy the <b>Client ID</b> and <b>Client Secret</b></li>" +
                "<li>Paste them in the fields above</li>" +
                "</ol>" +
                "</body></html>"
        );
        helpText.setEditable(false);
        helpText.setOpaque(false);
        helpText.setBorder(null);
        helpText.setAlignmentX(Component.LEFT_ALIGNMENT);
        helpText.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(e.getURL());
            }
        });
        helpPanel.add(helpText);

        mainPanel.add(helpPanel);

        add(mainPanel, BorderLayout.NORTH);

        // Set default selection and update field states
        useDefaultCredentialsRadio.setSelected(true);
        updateCustomFieldsState();
    }

    private JPanel createCustomCredentialsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Client ID:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        customClientIdField = new JBTextField();
        customClientIdField.setColumns(50);
        panel.add(customClientIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Client Secret:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        customClientSecretField = new JBPasswordField();
        customClientSecretField.setColumns(50);
        panel.add(customClientSecretField, gbc);

        // Set maximum size to prevent BoxLayout from collapsing it
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));

        return panel;
    }

    private void updateCustomFieldsState() {
        boolean customEnabled = useCustomCredentialsRadio.isSelected();
        customClientIdField.setEnabled(customEnabled);
        customClientSecretField.setEnabled(customEnabled);
    }

    public void reset(@NotNull GoogleOAuthClientConfigStore store) {
        useDefaultCredentialsRadio.setSelected(!store.isUseCustomCredentials());
        useCustomCredentialsRadio.setSelected(store.isUseCustomCredentials());

        customClientIdField.setText(store.getCustomClientId() != null ? store.getCustomClientId() : "");
        customClientSecretField.setText(store.getCustomClientSecret() != null ? store.getCustomClientSecret() : "");

        updateCustomFieldsState();
    }

    public boolean isModified(@NotNull GoogleOAuthClientConfigStore store) {
        boolean currentUseCustom = useCustomCredentialsRadio.isSelected();
        if (currentUseCustom != store.isUseCustomCredentials()) {
            return true;
        }

        if (currentUseCustom) {
            String currentClientId = customClientIdField.getText().trim();
            String currentClientSecret = new String(customClientSecretField.getPassword()).trim();

            String savedClientId = store.getCustomClientId();
            String savedClientSecret = store.getCustomClientSecret();

            if (!currentClientId.equals(savedClientId != null ? savedClientId : "")) {
                return true;
            }
            if (!currentClientSecret.equals(savedClientSecret != null ? savedClientSecret : "")) {
                return true;
            }
        }

        return false;
    }

    public void validateSettings() throws ConfigurationException {
        if (useCustomCredentialsRadio.isSelected()) {
            String clientId = customClientIdField.getText().trim();
            String clientSecret = new String(customClientSecretField.getPassword()).trim();

            if (clientId.isEmpty()) {
                throw new ConfigurationException("Client ID is required when using custom credentials");
            }
            if (clientSecret.isEmpty()) {
                throw new ConfigurationException("Client Secret is required when using custom credentials");
            }
        }
    }

    public void applyTo(@NotNull GoogleOAuthClientConfigStore store) {
        store.setUseCustomCredentials(useCustomCredentialsRadio.isSelected());

        if (useCustomCredentialsRadio.isSelected()) {
            store.setCustomClientId(customClientIdField.getText().trim());
            store.setCustomClientSecret(new String(customClientSecretField.getPassword()).trim());
        } else {
            store.clearCustomConfig();
        }
    }
}
