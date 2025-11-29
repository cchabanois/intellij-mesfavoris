package mesfavoris.internal.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarksStoreDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Action to connect to a specific remote bookmarks store.
 * Uses the icon from the store's descriptor.
 * Behaves as a toggle button: pressed when connected, normal when disconnected.
 */
public class ConnectToRemoteBookmarksStoreAction extends AnAction implements DumbAware, Toggleable {
    private static final Logger LOG = Logger.getInstance(ConnectToRemoteBookmarksStoreAction.class);
    
    private final IRemoteBookmarksStore store;

    public ConnectToRemoteBookmarksStoreAction(@NotNull IRemoteBookmarksStore store) {
        super();
        this.store = store;

        RemoteBookmarksStoreDescriptor descriptor = store.getDescriptor();
        // Set text for tooltip (will show only icon in toolbar)
        getTemplatePresentation().setText("Connect to %s".formatted(descriptor.label()));
        getTemplatePresentation().setDescription("Connect to %s remote bookmarks store".formatted(descriptor.label()));
        getTemplatePresentation().setIcon(descriptor.icon());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        boolean enabled = project != null && store.getState() != IRemoteBookmarksStore.State.connecting;
        event.getPresentation().setEnabled(enabled);

        // Set toggle state based on connection state
        boolean isConnected = store.getState() == IRemoteBookmarksStore.State.connected;
        Toggleable.setSelected(event.getPresentation(), isConnected);

        // Update text (tooltip) and description based on connection state
        RemoteBookmarksStoreDescriptor descriptor = store.getDescriptor();
        switch (store.getState()) {
            case connected:
                event.getPresentation().setText("Disconnect from %s".formatted(descriptor.label()));
                event.getPresentation().setDescription("Disconnect from %s remote bookmarks store".formatted(descriptor.label()));
                break;
            case connecting:
                event.getPresentation().setText("Connecting to %s...".formatted(descriptor.label()));
                event.getPresentation().setDescription("Connecting to %s...".formatted(descriptor.label()));
                break;
            case disconnected:
            default:
                event.getPresentation().setText("Connect to %s".formatted(descriptor.label()));
                event.getPresentation().setDescription("Connect to %s remote bookmarks store".formatted(descriptor.label()));
                break;
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        if (store.getState() == IRemoteBookmarksStore.State.connected) {
            disconnect(project);
        } else {
            connect(project);
        }
    }

    private void connect(@NotNull Project project) {
        RemoteBookmarksStoreDescriptor descriptor = store.getDescriptor();
        ProgressManager.getInstance().run(new Task.Modal(project, "Connecting to %s".formatted(descriptor.label()), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Connecting to %s...".formatted(descriptor.label()));
                    store.connect(indicator);
                    showConnectionSuccessMessage(project, descriptor);
                } catch (IOException e) {
                    LOG.error("Failed to connect to " + descriptor.label(), e);
                    showConnectionErrorMessage(project, descriptor, e);
                }
            }
        });
    }

    private void disconnect(@NotNull Project project) {
        RemoteBookmarksStoreDescriptor descriptor = store.getDescriptor();
        ProgressManager.getInstance().run(new Task.Modal(project, "Disconnecting from %s".formatted(descriptor.label()), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Disconnecting from %s...".formatted(descriptor.label()));
                    store.disconnect(indicator);
                    showDisconnectionSuccessMessage(project, descriptor);
                } catch (IOException e) {
                    LOG.error("Failed to disconnect from %s".formatted(descriptor.label()), e);
                    showDisconnectionErrorMessage(project, descriptor, e);
                }
            }
        });
    }

    private void showConnectionSuccessMessage(@NotNull Project project, @NotNull RemoteBookmarksStoreDescriptor descriptor) {
        ApplicationManager.getApplication().invokeLater(() -> {
            String userInfo = "";
            if (store.getUserInfo() != null) {
                String displayName = store.getUserInfo().getDisplayName();
                String email = store.getUserInfo().getEmailAddress();
                if (displayName != null && email != null) {
                    userInfo = " as %s (%s)".formatted(displayName, email);
                } else if (displayName != null) {
                    userInfo = " as " + displayName;
                } else if (email != null) {
                    userInfo = " as " + email;
                }
            }
            Notification notification = new Notification(
                    "com.cchabanois.mesfavoris.info",
                    "Connected to %s".formatted(descriptor.label()),
                    "Successfully connected%s".formatted(userInfo),
                    NotificationType.INFORMATION
            );
            Notifications.Bus.notify(notification, project);
        });
    }

    private void showConnectionErrorMessage(@NotNull Project project, @NotNull RemoteBookmarksStoreDescriptor descriptor, @NotNull IOException e) {
        ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(
                project,
                "Failed to connect to %s: %s".formatted(descriptor.label(), e.getMessage()),
                "Connection Failed"
        ));
    }

    private void showDisconnectionSuccessMessage(@NotNull Project project, @NotNull RemoteBookmarksStoreDescriptor descriptor) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = new Notification(
                    "com.cchabanois.mesfavoris.info",
                    "Disconnected from %s".formatted(descriptor.label()),
                    "Successfully disconnected",
                    NotificationType.INFORMATION
            );
            Notifications.Bus.notify(notification, project);
        });
    }

    private void showDisconnectionErrorMessage(@NotNull Project project, @NotNull RemoteBookmarksStoreDescriptor descriptor, @NotNull IOException e) {
        ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(
                project,
                "Failed to disconnect from %s: %s".formatted(descriptor.label(), e.getMessage()),
                "Disconnection Failed"
        ));
    }
}

