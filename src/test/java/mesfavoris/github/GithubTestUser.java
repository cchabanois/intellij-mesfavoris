package mesfavoris.github;

import java.util.Optional;

public enum GithubTestUser {

    USER1;

    public Optional<String> getToken() {
        // A missing GitHub secret in CI surfaces as an empty (not absent) env var, so treat blank as absent —
        // otherwise tests would take the real-API path with an empty token and fail with "Invalid GitHub token".
        String token = System.getenv(name() + "_GITHUB_TOKEN");
        return token == null || token.isBlank() ? Optional.empty() : Optional.of(token);
    }

    public String getApiBaseUrl() {
        String url = System.getenv(name() + "_GITHUB_API_URL");
        return url != null ? url : "https://api.github.com";
    }

}
