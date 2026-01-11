package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Action to swap keyboard shortcuts between Mesfavoris bookmark actions and IntelliJ bookmark actions.
 * This allows users to use either F11/Shift+F11 (IntelliJ defaults) or Alt+=/Shift+Alt+= (Mesfavoris defaults).
 */
public class SwapBookmarkShortcutsAction extends AnAction {

    private static final String INTELLIJ_TOGGLE_BOOKMARK = "ToggleBookmark";
    private static final String INTELLIJ_SHOW_BOOKMARKS = "ShowBookmarks";
    private static final String MESFAVORIS_ADD_BOOKMARK = "mesfavoris.actions.AddBookmarkAction";
    private static final String MESFAVORIS_SHOW_BOOKMARKS = "mesfavoris.actions.ShowBookmarksAction";

    public SwapBookmarkShortcutsAction() {
        super("Swap Shortcuts with IntelliJ Bookmarks",
              "Exchange keyboard shortcuts between Mes Favoris and IntelliJ bookmark actions",
              null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        KeymapManager keymapManager = KeymapManager.getInstance();
        if (keymapManager == null) {
            return;
        }

        Keymap activeKeymap = keymapManager.getActiveKeymap();

        // Get current shortcuts
        Shortcut[] intellijToggleShortcuts = activeKeymap.getShortcuts(INTELLIJ_TOGGLE_BOOKMARK);
        Shortcut[] intellijShowShortcuts = activeKeymap.getShortcuts(INTELLIJ_SHOW_BOOKMARKS);
        Shortcut[] mesfavorisAddShortcuts = activeKeymap.getShortcuts(MESFAVORIS_ADD_BOOKMARK);
        Shortcut[] mesfavorisShowShortcuts = activeKeymap.getShortcuts(MESFAVORIS_SHOW_BOOKMARKS);

        // Remove all current shortcuts
        removeAllShortcuts(activeKeymap, INTELLIJ_TOGGLE_BOOKMARK, intellijToggleShortcuts);
        removeAllShortcuts(activeKeymap, INTELLIJ_SHOW_BOOKMARKS, intellijShowShortcuts);
        removeAllShortcuts(activeKeymap, MESFAVORIS_ADD_BOOKMARK, mesfavorisAddShortcuts);
        removeAllShortcuts(activeKeymap, MESFAVORIS_SHOW_BOOKMARKS, mesfavorisShowShortcuts);

        // Swap shortcuts
        addAllShortcuts(activeKeymap, INTELLIJ_TOGGLE_BOOKMARK, mesfavorisAddShortcuts);
        addAllShortcuts(activeKeymap, INTELLIJ_SHOW_BOOKMARKS, mesfavorisShowShortcuts);
        addAllShortcuts(activeKeymap, MESFAVORIS_ADD_BOOKMARK, intellijToggleShortcuts);
        addAllShortcuts(activeKeymap, MESFAVORIS_SHOW_BOOKMARKS, intellijShowShortcuts);

        // Show confirmation message
        String message = buildConfirmationMessage(intellijToggleShortcuts, mesfavorisAddShortcuts);
        Messages.showInfoMessage(message, "Bookmark Shortcuts Swapped");
    }

    private void removeAllShortcuts(Keymap keymap, String actionId, Shortcut[] shortcuts) {
        for (Shortcut shortcut : shortcuts) {
            keymap.removeShortcut(actionId, shortcut);
        }
    }

    private void addAllShortcuts(Keymap keymap, String actionId, Shortcut[] shortcuts) {
        for (Shortcut shortcut : shortcuts) {
            keymap.addShortcut(actionId, shortcut);
        }
    }

    private String buildConfirmationMessage(Shortcut[] intellijShortcuts, Shortcut[] mesfavorisShortcuts) {
        String intellijShortcutText = formatShortcuts(intellijShortcuts);
        String mesfavorisShortcutText = formatShortcuts(mesfavorisShortcuts);

        return String.format(
            "Bookmark shortcuts have been swapped:\n\n" +
            "IntelliJ Bookmarks now use: %s\n" +
            "Mes Favoris now use: %s",
            mesfavorisShortcutText.isEmpty() ? "(none)" : mesfavorisShortcutText,
            intellijShortcutText.isEmpty() ? "(none)" : intellijShortcutText
        );
    }

    private String formatShortcuts(Shortcut[] shortcuts) {
        if (shortcuts.length == 0) {
            return "";
        }
        return Arrays.stream(shortcuts)
            .map(com.intellij.openapi.keymap.KeymapUtil::getShortcutText)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}

