package mesfavoris.gdrive;

import com.intellij.openapi.util.IconLoader;
import mesfavoris.icons.MesFavorisIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Icons for Google Drive integration
 */
public class GDriveIcons {

    /**
     * Main Google Drive icon
     */
    public static final @NotNull Icon GDRIVE = load("/icons/gdrive.png");

    /**
     * Overlay icon for Google Drive bookmarks (using a smaller icon)
     */
    public static final @NotNull Icon GDRIVE_OVERLAY = load("/icons/gdrive_overlay.png");

    private static @NotNull Icon load(@NotNull String path) {
        return IconLoader.getIcon(path, MesFavorisIcons.class);
    }

}

