package mesfavoris.gdrive.connection;

import org.jetbrains.annotations.NotNull;

/**
 * Holds Google OAuth client configuration (client ID and client secret).
 * Provides default configuration embedded in the plugin and supports custom configuration.
 */
public class GoogleOAuthClientConfig {
    // Default credentials embedded in the plugin
    // Note: In OAuth for installed applications, the client secret is not treated as a secret
    // because it's embedded in the application and can be extracted by users.
    private static final String DEFAULT_CLIENT_ID = "788843639556-5prg6m9hnj3pl2o2f29cj3049uuss7hm.apps.googleusercontent.com";
    private static final String DEFAULT_CLIENT_SECRET = "t_EIMZeIW8y-yxIolGEnNL2k";

    private final String clientId;
    private final String clientSecret;

    public GoogleOAuthClientConfig(@NotNull String clientId, @NotNull String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Get the default configuration embedded in the plugin.
     * These credentials will show a Google security warning because the app is not verified.
     *
     * @return default Google OAuth client configuration
     */
    @NotNull
    public static GoogleOAuthClientConfig getDefault() {
        return new GoogleOAuthClientConfig(DEFAULT_CLIENT_ID, DEFAULT_CLIENT_SECRET);
    }

    @NotNull
    public String getClientId() {
        return clientId;
    }

    @NotNull
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Check if this is the default configuration
     */
    public boolean isDefault() {
        return DEFAULT_CLIENT_ID.equals(clientId) && DEFAULT_CLIENT_SECRET.equals(clientSecret);
    }
}

