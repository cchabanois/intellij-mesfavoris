package mesfavoris.icons;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MesFavorisIcons {

    private static @NotNull Icon load(@NotNull String path) {
        return IconLoader.getIcon(path, MesFavorisIcons.class);
    }

    public static final @NotNull Icon toolWindowMesFavoris = load("/icons/toolWindowMesFavoris.png");

    public static final @NotNull Icon bookmark = load("/icons/bookmark-16.png");

    public static final @NotNull Icon bookmarks = load("/icons/bookmarks-16.png");

    public static final @NotNull Icon snippet = load("/icons/snippet.png");

    public static final @NotNull Icon VIRTUAL_OVERLAY = load("/icons/virtual_overlay.png");

}
