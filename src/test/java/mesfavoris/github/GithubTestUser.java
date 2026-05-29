package mesfavoris.github;

import java.util.Optional;

public enum GithubTestUser {

    USER1;

    public Optional<String> getToken() {
        return Optional.ofNullable(System.getenv(name() + "_GITHUB_TOKEN"));
    }

    public String getApiBaseUrl() {
        String url = System.getenv(name() + "_GITHUB_API_URL");
        return url != null ? url : "https://api.github.com";
    }

}
