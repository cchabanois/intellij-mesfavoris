package mesfavoris.gdrive.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import mesfavoris.gdrive.connection.GoogleOAuthClientConfigStore;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for Google Drive credentials settings
 */
public class GoogleDriveConfigurable implements Configurable {

    private final Project project;
    private final GoogleOAuthClientConfigStore store;
    private GoogleDriveCredentialsPanel panel;

    public GoogleDriveConfigurable(Project project) {
        this.project = project;
        this.store = project.getService(GoogleOAuthClientConfigStore.class);
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Google Drive";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (panel == null) {
            panel = new GoogleDriveCredentialsPanel(project);
            reset(); // Load actual data
        }
        return panel;
    }

    @Override
    public boolean isModified() {
        if (panel == null) {
            return false;
        }
        return panel.isModified(store);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (panel == null) {
            return;
        }

        // Validate before applying
        panel.validateSettings();

        // Apply settings
        panel.applyTo(store);
    }

    @Override
    public void reset() {
        if (panel == null) {
            return;
        }
        panel.reset(store);
    }

    @Override
    public void disposeUIResources() {
        panel = null;
    }
}

