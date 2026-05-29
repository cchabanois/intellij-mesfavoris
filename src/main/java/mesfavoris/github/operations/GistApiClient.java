package mesfavoris.github.operations;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import mesfavoris.remote.ConflictException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Low-level HTTP client for the GitHub Gist REST API.
 */
public class GistApiClient {
    private static final String DEFAULT_BASE_URL = "https://api.github.com";
    private static final String API_VERSION = "2022-11-28";

    private final HttpClient httpClient;
    private final Gson gson;
    private final Supplier<String> tokenSupplier;
    private final Supplier<String> baseUrlSupplier;
    private final String userAgent;

    public GistApiClient(Supplier<String> tokenSupplier) {
        this(tokenSupplier, () -> DEFAULT_BASE_URL, HttpClient.newHttpClient(), "");
    }

    public GistApiClient(Supplier<String> tokenSupplier, HttpClient httpClient) {
        this(tokenSupplier, () -> DEFAULT_BASE_URL, httpClient, "");
    }

    public GistApiClient(Supplier<String> tokenSupplier, Supplier<String> baseUrlSupplier,
                         HttpClient httpClient) {
        this(tokenSupplier, baseUrlSupplier, httpClient, "");
    }

    public GistApiClient(Supplier<String> tokenSupplier, Supplier<String> baseUrlSupplier,
                         HttpClient httpClient, String userAgent) {
        this.tokenSupplier = tokenSupplier;
        this.baseUrlSupplier = baseUrlSupplier;
        this.httpClient = httpClient;
        this.userAgent = userAgent;
        this.gson = new Gson();
    }

    public GistResponse createGist(String description, String fileName, String content) throws IOException {
        String body = gson.toJson(Map.of(
                "description", description,
                "public", false,
                "files", Map.of(fileName, Map.of("content", content))
        ));
        HttpRequest request = authorizedRequest(baseUrlSupplier.get() + "/gists")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 201) {
            throw new IOException("Failed to create gist: HTTP " + response.statusCode() + " " + response.body());
        }
        return gson.fromJson(response.body(), GistResponse.class);
    }

    public GistResponse updateGist(String gistId, String fileName, String content,
                                   @Nullable String expectedUpdatedAt)
            throws IOException, ConflictException {
        if (expectedUpdatedAt != null) {
            GistResponse current = loadGist(gistId);
            if (!expectedUpdatedAt.equals(current.updated_at)) {
                throw new ConflictException();
            }
        }
        String body = gson.toJson(Map.of(
                "files", Map.of(fileName, Map.of("content", content))
        ));
        HttpRequest request = authorizedRequest(baseUrlSupplier.get() + "/gists/" + gistId)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new IOException("Failed to update gist: HTTP " + response.statusCode() + " " + response.body());
        }
        return gson.fromJson(response.body(), GistResponse.class);
    }

    public GistResponse loadGist(String gistId) throws IOException {
        HttpRequest request = authorizedRequest(baseUrlSupplier.get() + "/gists/" + gistId)
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new IOException("Failed to load gist: HTTP " + response.statusCode() + " " + response.body());
        }
        return gson.fromJson(response.body(), GistResponse.class);
    }

    public String fetchRawContent(String rawUrl) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(rawUrl))
                .header("User-Agent", userAgent)
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch raw gist content: HTTP " + response.statusCode());
        }
        return response.body();
    }

    public void deleteGist(String gistId) throws IOException {
        HttpRequest request = authorizedRequest(baseUrlSupplier.get() + "/gists/" + gistId)
                .DELETE()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 204 && response.statusCode() != 404) {
            throw new IOException("Failed to delete gist: HTTP " + response.statusCode() + " " + response.body());
        }
    }

    @Nullable
    public String conditionalGetEtag(String gistId, @Nullable String ifNoneMatch) throws IOException {
        HttpRequest.Builder builder = authorizedRequest(baseUrlSupplier.get() + "/gists/" + gistId).GET();
        if (ifNoneMatch != null) {
            builder.header("If-None-Match", ifNoneMatch);
        }
        HttpRequest request = builder.build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == 304) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw new IOException("Failed to check gist: HTTP " + response.statusCode());
        }
        return response.headers().firstValue("ETag").orElse(null);
    }

    public List<GistResponse> listGists() throws IOException {
        List<GistResponse> all = new ArrayList<>();
        String url = baseUrlSupplier.get() + "/gists?per_page=100";
        while (url != null) {
            HttpRequest request = authorizedRequest(url).GET().build();
            HttpResponse<String> response = send(request);
            if (response.statusCode() != 200) {
                throw new IOException("Failed to list gists: HTTP " + response.statusCode());
            }
            all.addAll(Arrays.asList(gson.fromJson(response.body(), GistResponse[].class)));
            url = nextPageUrl(response.headers().firstValue("Link").orElse(null));
        }
        return all;
    }

    private static String nextPageUrl(String linkHeader) {
        if (linkHeader == null) return null;
        for (String part : linkHeader.split(",")) {
            if (part.contains("rel=\"next\"")) {
                int start = part.indexOf('<') + 1;
                int end = part.indexOf('>');
                if (start > 0 && end > start) return part.substring(start, end);
            }
        }
        return null;
    }

    public UserResponse getAuthenticatedUser() throws IOException {
        HttpRequest request = authorizedRequest(baseUrlSupplier.get() + "/user").GET().build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == 401) {
            throw new IOException("Invalid GitHub token");
        }
        if (response.statusCode() != 200) {
            throw new IOException("Failed to get authenticated user: HTTP " + response.statusCode());
        }
        return gson.fromJson(response.body(), UserResponse.class);
    }

    private HttpRequest.Builder authorizedRequest(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + tokenSupplier.get())
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", API_VERSION)
                .header("User-Agent", userAgent)
                .header("Content-Type", "application/json");
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    // Response POJOs

    public static class GistResponse {
        public String id;
        public String description;
        public String updated_at;
        public String html_url;
        public GistOwner owner;
        public Map<String, GistFile> files;

        public static class GistOwner {
            public String login;
        }
    }

    public static class GistFile {
        public String content;
        public String raw_url;
        @SerializedName("truncated")
        public Boolean truncated;
    }

    public static class UserResponse {
        public String login;
        public String name;
    }
}
