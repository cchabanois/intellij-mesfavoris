package mesfavoris.internal.actions;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBusConnection;
import mesfavoris.internal.settings.MesFavorisSettingsListener;
import mesfavoris.internal.settings.MesFavorisSettingsStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Application-level service that replaces IntelliJ bookmark actions with Mesfavoris actions when enabled in settings.
 * Listens to settings changes via MessageBus to apply changes immediately.
 *
 * Initialized early at application startup via KeymapConfiguratorInitializer.
 */
public final class KeymapConfigurator implements Disposable {
    private static final Logger LOG = Logger.getInstance(KeymapConfigurator.class);

    private static final String INTELLIJ_TOGGLE_BOOKMARK = "ToggleBookmark";
    private static final String INTELLIJ_SHOW_BOOKMARKS = "ShowBookmarks";
    private static final String MESFAVORIS_SHOW_BOOKMARKS = "mesfavoris.actions.ShowBookmarksAction";
    private static final String ACTIVATE_TOOL_WINDOW_ACTION = "ActivatemesfavorisToolWindow";

    private AnAction originalToggleBookmark = null;
    private AnAction originalShowBookmarks = null;
    private boolean actionsReplaced = false;
    private MessageBusConnection messageBusConnection;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public KeymapConfigurator() {

    }

    public static KeymapConfigurator getInstance() {
        return ApplicationManager.getApplication().getService(KeymapConfigurator.class);
    }

    /**
     * Initialize the keymap configuration and subscribe to settings changes.
     * This method is called from ApplicationListener.appFrameCreated()
     * to ensure initialization happens early in the application lifecycle.
     *
     * This method is idempotent and thread-safe - calling it multiple times has no effect.
     */
    public void init() {
        // Use compareAndSet for thread-safe initialization
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        // Subscribe to settings changes
        messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        messageBusConnection.subscribe(MesFavorisSettingsListener.TOPIC,
            enabled -> configureActions());

        // Configure actions based on current settings
        configureActions();
    }

    @Override
    public void dispose() {
        // MessageBusConnection will be automatically disconnected because we passed 'this' as parent Disposable
        // when calling connect(this)
    }

    private void configureActions() {
        MesFavorisSettingsStore settings = MesFavorisSettingsStore.getInstance();
        boolean useIntellijShortcuts = settings.isUseIntellijBookmarkShortcuts();

        if (useIntellijShortcuts && !actionsReplaced) {
            replaceIntellijActionsWithOurActions();
        } else if (!useIntellijShortcuts && actionsReplaced) {
            restoreIntellijActions();
        }
        // we still need to copy them to the tool window action so that the tooltip displays the shortcut
        copyShortcutsToToolWindowAction(settings.isUseIntellijBookmarkShortcuts()
                ? INTELLIJ_SHOW_BOOKMARKS
                : MESFAVORIS_SHOW_BOOKMARKS);
    }

    private void replaceIntellijActionsWithOurActions() {
        ActionManager actionManager = ActionManager.getInstance();

        // Save original actions
        originalToggleBookmark = actionManager.getAction(INTELLIJ_TOGGLE_BOOKMARK);
        originalShowBookmarks = actionManager.getAction(INTELLIJ_SHOW_BOOKMARKS);

        // Unregister original actions
        if (originalToggleBookmark != null) {
            actionManager.unregisterAction(INTELLIJ_TOGGLE_BOOKMARK);
        }
        if (originalShowBookmarks != null) {
            actionManager.unregisterAction(INTELLIJ_SHOW_BOOKMARKS);
        }

        // Register Mesfavoris actions with IntelliJ action IDs
        actionManager.registerAction(INTELLIJ_TOGGLE_BOOKMARK, new AddBookmarkAction());
        actionManager.registerAction(INTELLIJ_SHOW_BOOKMARKS, new ShowBookmarksAction());

        actionsReplaced = true;
        LOG.info("Replaced IntelliJ bookmark actions with Mesfavoris actions");
    }

    private void restoreIntellijActions() {
        ActionManager actionManager = ActionManager.getInstance();

        // Unregister Mesfavoris actions
        actionManager.unregisterAction(INTELLIJ_TOGGLE_BOOKMARK);
        actionManager.unregisterAction(INTELLIJ_SHOW_BOOKMARKS);

        // Restore original actions
        if (originalToggleBookmark != null) {
            actionManager.registerAction(INTELLIJ_TOGGLE_BOOKMARK, originalToggleBookmark);
        }
        if (originalShowBookmarks != null) {
            actionManager.registerAction(INTELLIJ_SHOW_BOOKMARKS, originalShowBookmarks);
        }

        actionsReplaced = false;
        LOG.info("Restored original IntelliJ bookmark actions");
    }

    private void copyShortcutsToToolWindowAction(String sourceActionId) {
        KeymapManager keymapManager = KeymapManager.getInstance();
        if (keymapManager == null) {
            return;
        }

        Keymap activeKeymap = keymapManager.getActiveKeymap();

        // First, remove any existing shortcuts from the tool window action
        Shortcut[] existingShortcuts = activeKeymap.getShortcuts(ACTIVATE_TOOL_WINDOW_ACTION);
        for (Shortcut shortcut : existingShortcuts) {
            activeKeymap.removeShortcut(ACTIVATE_TOOL_WINDOW_ACTION, shortcut);
        }

        Shortcut[] shortcuts = activeKeymap.getShortcuts(sourceActionId);

        if (shortcuts.length > 0) {
            // Copy all shortcuts to the tool window activation action
            for (Shortcut shortcut : shortcuts) {
                activeKeymap.addShortcut(ACTIVATE_TOOL_WINDOW_ACTION, shortcut);
            }
            LOG.info("Copied " + shortcuts.length + " shortcuts from " + sourceActionId + " to " + ACTIVATE_TOOL_WINDOW_ACTION);
        }

        // Force refresh of tool window stripe tooltips
        refreshToolWindowStripes();
    }

    private void refreshToolWindowStripes() {
        // Iterate through all open projects and force tool window stripe refresh
        ApplicationManager.getApplication().invokeLater(() -> {
            Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : openProjects) {
                ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                ToolWindow toolWindow = toolWindowManager.getToolWindow("mesfavoris");
                if (toolWindow != null) {
                    // Force stripe button update by temporarily hiding and showing
                    // This is a workaround to refresh the tooltip
                    boolean wasVisible = toolWindow.isVisible();
                    toolWindow.setAvailable(false);
                    toolWindow.setAvailable(true);
                    if (wasVisible) {
                        toolWindow.show(null);
                    }
                }
            }
        });
    }

    /**
     * Application lifecycle listener that forces initialization of KeymapConfigurator early.
     * This ensures shortcuts are properly configured before tool windows are created.
     */
    public static final class ApplicationListener implements AppLifecycleListener {
        @Override
        public void appFrameCreated(@NotNull List<String> commandLineArgs) {
            // Initialize KeymapConfigurator early, before tool windows are created
            // This ensures shortcuts are properly configured when tool window tooltips are generated
            KeymapConfigurator.getInstance().init();
        }
    }

}

