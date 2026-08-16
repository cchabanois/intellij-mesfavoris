package mesfavoris.github.client;

import com.google.gson.Gson;
import mesfavoris.remote.ConflictException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Low-level HTTP client for the GitHub Gist REST API. Implements {@link IGistApiClient} so callers can
 * depend on the interface and be unit-tested with a mock.
 */
public class GistApiClient implements IGistApiClient {
    private static final String DEFAULT_BASE_URL = "https://api.github.com";
    private static final String API_VERSION = "2022-11-28";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    /** GitHub asks clients to wait at least one second between mutative requests (secondary rate limit). */
    private static final Duration MIN_WRITE_INTERVAL = Duration.ofSeconds(1);

    public static HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private final HttpClient httpClient;
    private final Gson gson;
    private final Supplier<String> tokenSupplier;
    private final Supplier<String> baseUrlSupplier;
    private final String userAgent;
    @Nullable
    private final IGistFileContentProvider contentProvider;
    private final Object writeGate = new Object();
    private long lastWriteNanos = 0;

    public GistApiClient(Supplier<String> tokenSupplier) {
        this(tokenSupplier, () -> DEFAULT_BASE_URL, newHttpClient(), "");
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
        this(tokenSupplier, baseUrlSupplier, httpClient, userAgent, null);
    }

    /**
     * @param contentProvider recovers full file content (see {@link #getFileContent}); may be {@code null}
     *                        for clients that only perform REST operations and never fetch file content.
     */
    public GistApiClient(Supplier<String> tokenSupplier, Supplier<String> baseUrlSupplier,
                         HttpClient httpClient, String userAgent,
                         @Nullable IGistFileContentProvider contentProvider) {
        this.tokenSupplier = tokenSupplier;
        this.baseUrlSupplier = baseUrlSupplier;
        this.httpClient = httpClient;
        this.userAgent = userAgent;
        this.contentProvider = contentProvider;
        this.gson = new Gson();
    }

    /** The OAuth token used to authenticate against the API; needed to authenticate git clones of a Gist. */
    @Override
    public String getToken() {
        return tokenSupplier.get();
    }

    @Override
    public byte[] getFileContent(GistResponse gist, GistFile file) throws IOException {
        if (contentProvider == null) {
            throw new IOException("This GistApiClient was created without a content provider");
        }
        return contentProvider.getFileContent(gist, file);
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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
    @Override
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

    @Override
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

    @Override
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
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + tokenSupplier.get())
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", API_VERSION)
                .header("User-Agent", userAgent)
                .header("Content-Type", "application/json");
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        throttleIfMutating(request.method());
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Spaces mutative requests (POST/PATCH/PUT/DELETE) by at least {@link #MIN_WRITE_INTERVAL} to avoid
     * tripping GitHub's secondary rate limit; GET/HEAD are not delayed. Only back-to-back writes wait, so
     * ordinary single operations are unaffected.
     */
    private void throttleIfMutating(String method) {
        if (!isMutating(method)) {
            return;
        }
        synchronized (writeGate) {
            long waitNanos = MIN_WRITE_INTERVAL.toNanos() - (System.nanoTime() - lastWriteNanos);
            if (lastWriteNanos != 0L && waitNanos > 0) {
                sleep(Duration.ofNanos(waitNanos));
            }
            lastWriteNanos = System.nanoTime();
        }
    }

    private static boolean isMutating(String method) {
        return method.equals("POST") || method.equals("PATCH")
                || method.equals("PUT") || method.equals("DELETE");
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
