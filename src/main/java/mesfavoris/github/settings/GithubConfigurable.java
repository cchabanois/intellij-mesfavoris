package mesfavoris.github.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/** IntelliJ {@code Configurable} that exposes the GitHub Gists connection panel under Mes Favoris settings. */
public class GithubConfigurable implements Configurable {
    private final Project project;
    private GithubSettingsPanel panel;

    public GithubConfigurable(Project project) {
        this.project = project;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "GitHub Gists";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (panel == null) {
            panel = new GithubSettingsPanel(project);
        }
        return panel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() {
        // All actions (connect/disconnect) happen immediately in the panel
    }

    @Override
    public void reset() {
        // Status is refreshed when panel is created
    }

    @Override
    public void disposeUIResources() {
        panel = null;
    }
}
