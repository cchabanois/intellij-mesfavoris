package mesfavoris.gdrive.connection;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent store for Google OAuth client configuration.
 * Stores whether to use custom configuration and the custom client ID.
 * The custom client secret is stored securely in PasswordSafe.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "GoogleOAuthClientConfigStore",
    storages = @Storage("mesfavoris.xml")
)
public final class GoogleOAuthClientConfigStore implements PersistentStateComponent<Element> {
    private static final String ELEMENT_USE_CUSTOM = "useCustomCredentials";
    private static final String ELEMENT_CLIENT_ID = "customClientId";
    private static final String PASSWORD_SAFE_KEY = "GoogleDrive:CustomClientSecret";

    private final Project project;
    private boolean useCustomCredentials = false;
    private String customClientId = null;

    public GoogleOAuthClientConfigStore(@NotNull Project project) {
        this.project = project;
    }

    /**
     * Get the Google OAuth client configuration to use (either default or custom)
     */
    @NotNull
    public GoogleOAuthClientConfig getConfig() {
        if (useCustomCredentials && customClientId != null && !customClientId.trim().isEmpty()) {
            String customClientSecret = getCustomClientSecret();
            if (customClientSecret != null && !customClientSecret.trim().isEmpty()) {
                return new GoogleOAuthClientConfig(customClientId.trim(), customClientSecret.trim());
            }
        }
        return GoogleOAuthClientConfig.getDefault();
    }

    public boolean isUseCustomCredentials() {
        return useCustomCredentials;
    }

    public void setUseCustomCredentials(boolean useCustomCredentials) {
        this.useCustomCredentials = useCustomCredentials;
    }

    @Nullable
    public String getCustomClientId() {
        return customClientId;
    }

    public void setCustomClientId(@Nullable String customClientId) {
        this.customClientId = customClientId;
    }

    @Nullable
    public String getCustomClientSecret() {
        CredentialAttributes attributes = createCredentialAttributes();
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        return credentials != null ? credentials.getPasswordAsString() : null;
    }

    public void setCustomClientSecret(@Nullable String customClientSecret) {
        CredentialAttributes attributes = createCredentialAttributes();
        if (customClientSecret == null || customClientSecret.trim().isEmpty()) {
            PasswordSafe.getInstance().set(attributes, null);
        } else {
            Credentials credentials = new Credentials("custom", customClientSecret);
            PasswordSafe.getInstance().set(attributes, credentials);
        }
    }

    /**
     * Clear all custom configuration
     */
    public void clearCustomConfig() {
        this.customClientId = null;
        setCustomClientSecret(null);
        this.useCustomCredentials = false;
    }

    private CredentialAttributes createCredentialAttributes() {
        String serviceName = CredentialAttributesKt.generateServiceName(
            "GoogleDrive",
            project.getLocationHash() + ":" + PASSWORD_SAFE_KEY
        );
        return new CredentialAttributes(serviceName);
    }

    @Nullable
    @Override
    public Element getState() {
        Element element = new Element("GoogleOAuthClientConfigStore");
        element.setAttribute(ELEMENT_USE_CUSTOM, String.valueOf(useCustomCredentials));
        if (customClientId != null) {
            element.setAttribute(ELEMENT_CLIENT_ID, customClientId);
        }
        return element;
    }

    @Override
    public void loadState(@NotNull Element state) {
        String useCustomStr = state.getAttributeValue(ELEMENT_USE_CUSTOM);
        this.useCustomCredentials = Boolean.parseBoolean(useCustomStr);
        this.customClientId = state.getAttributeValue(ELEMENT_CLIENT_ID);
    }
}

