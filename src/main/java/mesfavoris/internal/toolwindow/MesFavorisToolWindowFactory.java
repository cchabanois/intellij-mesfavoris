package mesfavoris.internal.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.actions.CollapseAllAction;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapManagerListener;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import mesfavoris.internal.actions.*;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarksStoreExtensionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MesFavorisToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        final ToolWindowEx toolWindowEx = (ToolWindowEx) toolWindow;
        toolWindowEx.setTitleActions(getTitleActions(project));

        // Customize stripe button tooltip to include shortcut
        updateStripeButtonTooltip(toolWindow);

        // Listen to keymap changes and theme changes
        MessageBusConnection connection = com.intellij.openapi.application.ApplicationManager.getApplication().getMessageBus().connect();
        connection.subscribe(KeymapManagerListener.TOPIC, new KeymapManagerListener() {
            @Override
            public void activeKeymapChanged(@Nullable Keymap keymap) {
                updateStripeButtonTooltip(toolWindow);
            }

            @Override
            public void shortcutChanged(@NotNull Keymap keymap, @NotNull String actionId) {
                if (ShowBookmarksAction.ACTION_ID.equals(actionId)) {
                    updateStripeButtonTooltip(toolWindow);
                }
            }
        });

        connection.subscribe(LafManagerListener.TOPIC, source -> updateStripeButtonTooltip(toolWindow));

        // Dispose connection when tool window content is removed
        MesFavorisPanel panel = new MesFavorisPanel(project);
        Disposer.register(panel, connection);

        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(panel, null, false);
        contentManager.addContent(content);
        DataManager.registerDataProvider(panel, panel);
    }

    private void updateStripeButtonTooltip(@NotNull ToolWindow toolWindow) {
        KeymapManager keymapManager = KeymapManager.getInstance();
        if (keymapManager == null) {
            return;
        }

        Keymap activeKeymap = keymapManager.getActiveKeymap();
        Shortcut[] shortcuts = activeKeymap.getShortcuts(ShowBookmarksAction.ACTION_ID);

        if (shortcuts.length > 0) {
            // Display only the first shortcut in theme-aware gray color
            String shortcutText = KeymapUtil.getShortcutText(shortcuts[0]);
            Color grayColor = UIUtil.getLabelInfoForeground();
            String colorHex = ColorUtil.toHex(grayColor);
            toolWindow.setStripeTitle("<html>Mes Favoris <span style='color:#" + colorHex + ";'>" + shortcutText + "</span></html>");
        } else {
            toolWindow.setStripeTitle("Mes Favoris");
        }
    }

    private List<AnAction> getTitleActions(@NotNull Project project) {
        List<AnAction> actions = new ArrayList<>();

        actions.add(createSelectBookmarkAtCaretAction());
        actions.add(createRefreshRemoteFoldersAction());
        actions.add(createCollapseAllAction());
        actions.addAll(createRemoteBookmarksStoreActions(project));
        actions.add(createSettingsActionGroup());

        return actions;
    }

    private AnAction createSelectBookmarkAtCaretAction() {
        final SelectBookmarkAtCaretAction selectBookmarkAtCaretAction = new SelectBookmarkAtCaretAction();
        selectBookmarkAtCaretAction.getTemplatePresentation().setIcon(AllIcons.General.Locate);
        return selectBookmarkAtCaretAction;
    }

    private AnAction createRefreshRemoteFoldersAction() {
        final RefreshRemoteFoldersAction refreshAction = new RefreshRemoteFoldersAction();
        refreshAction.getTemplatePresentation().setIcon(AllIcons.Actions.Refresh);
        return refreshAction;
    }

    private AnAction createCollapseAllAction() {
        final CollapseAllAction collapseAction = new CollapseAllAction();
        collapseAction.getTemplatePresentation().setIcon(AllIcons.Actions.Collapseall);
        return collapseAction;
    }

    private List<AnAction> createRemoteBookmarksStoreActions(@NotNull Project project) {
        RemoteBookmarksStoreExtensionManager manager = project.getService(RemoteBookmarksStoreExtensionManager.class);
        List<IRemoteBookmarksStore> stores = manager.getStores();

        return stores.stream()
                .map(ConnectToRemoteBookmarksStoreAction::new)
                .collect(java.util.stream.Collectors.toList());
    }

    private AnAction createSettingsActionGroup() {
        final SettingsActionGroup settingsActionGroup = new SettingsActionGroup();
        settingsActionGroup.getTemplatePresentation().setIcon(AllIcons.General.Settings);
        return settingsActionGroup;
    }
}
