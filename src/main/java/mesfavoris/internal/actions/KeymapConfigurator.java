package mesfavoris.internal.actions;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.messages.MessageBusConnection;
import mesfavoris.internal.settings.MesFavorisSettingsListener;
import mesfavoris.internal.settings.MesFavorisSettingsStore;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Application-level service that replaces IntelliJ bookmark actions with Mesfavoris actions when enabled in settings.
 * Listens to settings changes via MessageBus to apply changes immediately.
 *
 * Initialized once at application startup via the Initializer PostStartupActivity.
 */
public final class KeymapConfigurator implements Disposable {
    private static final Logger LOG = Logger.getInstance(KeymapConfigurator.class);

    private static final String INTELLIJ_TOGGLE_BOOKMARK = "ToggleBookmark";
    private static final String INTELLIJ_SHOW_BOOKMARKS = "ShowBookmarks";

    private AnAction originalToggleBookmark = null;
    private AnAction originalShowBookmarks = null;
    private boolean actionsReplaced = false;
    private MessageBusConnection messageBusConnection;

    public static KeymapConfigurator getInstance() {
        return ApplicationManager.getApplication().getService(KeymapConfigurator.class);
    }

    /**
     * Initialize the keymap configuration and subscribe to settings changes.
     * Called once by the Initializer PostStartupActivity.
     */
    void init() {
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
    }

    private void configureActions() {
        MesFavorisSettingsStore settings = MesFavorisSettingsStore.getInstance();
        boolean replaceShortcuts = settings.isReplaceIntellijShortcuts();

        if (replaceShortcuts && !actionsReplaced) {
            replaceIntellijActions();
        } else if (!replaceShortcuts && actionsReplaced) {
            restoreIntellijActions();
        }
    }

    private void replaceIntellijActions() {
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

    /**
     * PostStartupActivity that initializes KeymapConfigurator once when the first project opens.
     * Uses an AtomicBoolean to ensure thread-safe initialization that happens only once across all projects.
     */
    public static final class Initializer implements StartupActivity.DumbAware {
        private static final AtomicBoolean initialized = new AtomicBoolean(false);

        @Override
        public void runActivity(@NotNull Project project) {
            // compareAndSet is atomic: only one thread will successfully change false to true
            if (initialized.compareAndSet(false, true)) {
                KeymapConfigurator.getInstance().init();
            }
        }
    }

}

