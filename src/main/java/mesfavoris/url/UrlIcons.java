package mesfavoris.url;

import com.intellij.openapi.util.IconLoader;
import mesfavoris.icons.MesFavorisIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class UrlIcons {

    private static @NotNull Icon load(@NotNull String path) {
        return IconLoader.getIcon(path, MesFavorisIcons.class);
    }

    /** 16x16 **/ public static final @NotNull Icon pageTagBlue = load("/icons/page_tag_blue.gif");


}
