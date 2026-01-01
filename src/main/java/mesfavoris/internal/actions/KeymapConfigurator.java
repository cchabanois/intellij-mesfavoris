package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import mesfavoris.internal.settings.MesFavorisSettingsStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Replaces IntelliJ bookmark actions with Mesfavoris actions when enabled in settings
 */
public class KeymapConfigurator implements ProjectActivity {
    private static final Logger LOG = Logger.getInstance(KeymapConfigurator.class);

    private static final String INTELLIJ_TOGGLE_BOOKMARK = "ToggleBookmark";
    private static final String INTELLIJ_SHOW_BOOKMARKS = "ShowBookmarks";

    private static AnAction originalToggleBookmark = null;
    private static AnAction originalShowBookmarks = null;
    private static boolean actionsReplaced = false;

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        configureActions();
        return null;
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
        actionManager.registerAction(INTELLIJ_SHOW_BOOKMARKS, new ShowMesFavorisToolWindowAction());

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
}

