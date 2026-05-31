package mesfavoris.github.settings;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import mesfavoris.github.BookmarksGithubService;
import mesfavoris.github.connection.GithubConnectionManager;
import mesfavoris.remote.IRemoteBookmarksStore.State;
import mesfavoris.remote.RemoteStoreConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;

/**
 * Settings panel for the GitHub Gists remote store.
 */
public class GithubSettingsPanel extends JPanel {
    private final Project project;
    private final JBLabel statusLabel;
    private final JButton connectButton;
    private final JButton disconnectButton;

    public GithubSettingsPanel(@NotNull Project project) {
        this.project = project;
        this.statusLabel = new JBLabel();
        this.connectButton = new JButton("Connect");
        this.disconnectButton = new JButton("Disconnect");

        buildUI();
        updateStatus();

        connectButton.addActionListener(e -> doConnect());
        disconnectButton.addActionListener(e -> doDisconnect());
    }

    private void buildUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = JBUI.insets(4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;

        JBLabel title = new JBLabel("GitHub Gists Authentication");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        add(title, c);

        c.gridy++;
        add(statusLabel, c);

        c.gridy++;
        c.gridwidth = 1;
        add(connectButton, c);
        c.gridx++;
        add(disconnectButton, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        JBLabel helpLabel = new JBLabel(
                "<html>Configure your GitHub account in <b>Settings &gt; Version Control &gt; GitHub</b>.<br>" +
                "Then click Connect to authorize access to your Gists.</html>");
        add(helpLabel, c);
    }

    private void updateStatus() {
        GithubConnectionManager manager = project.getService(BookmarksGithubService.class).getConnectionManager();
        State state = manager.getState();
        switch (state) {
            case connected -> {
                String login = manager.getUserInfo() != null ? manager.getUserInfo().getEmailAddress() : "unknown";
                statusLabel.setText("Status: Connected as " + login);
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
            }
            case connecting -> {
                statusLabel.setText("Status: Connecting...");
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(false);
            }
            default -> {
                statusLabel.setText("Status: Disconnected");
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
            }
        }
    }

    private void doConnect() {
        GithubConnectionManager manager = project.getService(BookmarksGithubService.class).getConnectionManager();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Connecting to GitHub") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    manager.connect(indicator);
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        if (e instanceof RemoteStoreConfigurationException ex) {
                            Notification notification = new Notification(
                                    "com.cchabanois.mesfavoris.errors",
                                    "GitHub Connection Failed",
                                    e.getMessage(),
                                    NotificationType.ERROR
                            );
                            notification.addAction(NotificationAction.createSimple(
                                    "Open Settings",
                                    () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, ex.getConfigurableClass())
                            ));
                            Notifications.Bus.notify(notification, project);
                        } else {
                            JOptionPane.showMessageDialog(GithubSettingsPanel.this,
                                    "Connection failed: " + e.getMessage(), "GitHub Connection", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                } finally {
                    SwingUtilities.invokeLater(GithubSettingsPanel.this::updateStatus);
                }
            }
        });
    }

    private void doDisconnect() {
        GithubConnectionManager manager = project.getService(BookmarksGithubService.class).getConnectionManager();
        manager.disconnect(null);
        updateStatus();
    }

}
