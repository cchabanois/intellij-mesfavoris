package mesfavoris.github.client.content;
import mesfavoris.github.client.IGistFileContentProvider;

import mesfavoris.github.client.GistFile;
import mesfavoris.github.client.GistResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Fetches the full file content over HTTP from the Gist {@code raw_url} (authenticated). Works for files
 * up to 10&nbsp;MB. Unlike the REST API operations, this hits the raw content host
 * ({@code gist.githubusercontent.com}), so the HTTP call lives here rather than in the API client.
 * <p>
 * The fetched content is validated against the truncated prefix the API already returned
 * ({@code file.content}); this rejects a login/error page served instead of the file — a host that
 * redirects unauthenticated raw requests returns HTML that does not start with the prefix, so this
 * provider throws and the next one (a git clone) takes over.
 */
public class HttpGetGistFileContentProvider implements IGistFileContentProvider {
    /** Chars dropped from the truncated prefix before matching, in case truncation split a multi-byte char. */
    private static final int PREFIX_MATCH_MARGIN = 16;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final Supplier<String> tokenSupplier;

    public HttpGetGistFileContentProvider(HttpClient httpClient, Supplier<String> tokenSupplier) {
        this.httpClient = httpClient;
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public byte[] getFileContent(GistResponse gist, GistFile file) throws IOException {
        if (file.raw_url == null) {
            throw new IOException("Gist file " + file.filename + " has no raw url");
        }
        String raw = fetchRawContent(file.raw_url);
        if (!startsWithTruncatedPrefix(raw, file.content)) {
            throw new IOException("Raw content of " + file.filename
                    + " is inconsistent with the API content (login or error page?)");
        }
        return raw.getBytes(StandardCharsets.UTF_8);
    }

    private String fetchRawContent(String rawUrl) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(rawUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + tokenSupplier.get())
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch raw gist content: HTTP " + response.statusCode());
        }
        return response.body();
    }

    /**
     * The API already returned the leading part of the file in {@code truncatedPrefix}; the full raw content
     * must start with it. Validates without assuming any content type. A small tail margin absorbs a
     * multi-byte character possibly split at the truncation boundary. Returns {@code false} when the prefix
     * is too short to verify, so the caller falls back to another provider.
     */
    private static boolean startsWithTruncatedPrefix(String raw, String truncatedPrefix) {
        if (raw == null || truncatedPrefix == null) {
            return false;
        }
        int compareLength = truncatedPrefix.length() - PREFIX_MATCH_MARGIN;
        return compareLength > 0
                && raw.length() >= compareLength
                && raw.regionMatches(0, truncatedPrefix, 0, compareLength);
    }
}
