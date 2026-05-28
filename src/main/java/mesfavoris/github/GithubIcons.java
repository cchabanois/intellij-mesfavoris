package mesfavoris.github;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import mesfavoris.icons.MesFavorisIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class GithubIcons {
    public static final @NotNull Icon GITHUB = AllIcons.Vcs.Vendors.Github;
    public static final @NotNull Icon GITHUB_OVERLAY = load("/icons/github_overlay.svg");

    private static @NotNull Icon load(@NotNull String path) {
        return IconLoader.getIcon(path, MesFavorisIcons.class);
    }
}
