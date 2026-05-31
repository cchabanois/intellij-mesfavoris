package mesfavoris.remote;

import com.intellij.openapi.options.Configurable;

import java.io.IOException;

/**
 * Thrown when a remote store cannot connect because it is not configured.
 * Carries the configurable class of the settings page that should be opened to fix the issue.
 */
public class RemoteStoreConfigurationException extends IOException {
    private final Class<? extends Configurable> configurableClass;

    public RemoteStoreConfigurationException(String message, Class<? extends Configurable> configurableClass) {
        super(message);
        this.configurableClass = configurableClass;
    }

    public Class<? extends Configurable> getConfigurableClass() {
        return configurableClass;
    }
}
