package mesfavoris.icons;

import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MesFavorisIcons {

    private static @NotNull Icon load(@NotNull String path) {
        return IconManager.getInstance().getIcon(path, MesFavorisIcons.class);
    }

    /** 13x13 */ public static final @NotNull Icon toolWindowMesFavoris = load("/icons/toolWindowMesFavoris.png");

    /** 16x16 **/ public static final @NotNull Icon bookmark = load("/icons/bookmark-16.png");

    /** 16x16 **/ public static final @NotNull Icon snippet = load("/icons/snippet.png");

}
