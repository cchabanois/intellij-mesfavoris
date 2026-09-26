package mesfavoris.github.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import mesfavoris.github.client.GistApiClient;
import mesfavoris.github.client.content.DefaultGistFileContentProvider;
import org.junit.rules.ExternalResource;

import java.net.http.HttpClient;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * A stateful, in-memory emulation of the subset of the GitHub Gist REST API that this plugin uses, served over
 * HTTP by an embedded WireMock server. Point a {@link GistApiClient} at {@link #baseUrl()} (or use
 * {@link #newApiClient()}) and it behaves as if talking to GitHub — create/load/update/delete round-trip, ETags
 * change on write and drive {@code If-None-Match} 304s, {@code updated_at} advances (second precision), and large
 * files are served truncated with a working {@code raw_url}. This lets the operation tests run offline, so they
 * never hit — and never get rate-limited or banned by — the real API.
 *
 * <p>Not covered: real auth (any token is accepted) and pagination of the gist list.
 */
public class FakeGistApiServer extends ExternalResource {

    /** Token handed to the API client; the fake accepts anything, this is just a placeholder. */
    public static final String TOKEN = "fake-token";

    /** Files larger than this (chars) are served truncated, forcing the raw_url recovery path. */
    private static final int TRUNCATE_THRESHOLD = 1_000_000;

    private final Gson gson = new Gson();
    private final Map<String, StoredGist> gists = new LinkedHashMap<>();
    private final AtomicInteger versionSeq = new AtomicInteger();

    private WireMockServer server;

    @Override
    protected void before() {
        start();
    }

    @Override
    protected void after() {
        stop();
    }

    /** Starts the embedded server. Called automatically when used as a JUnit {@code @Rule}. */
    public void start() {
        server = new WireMockServer(options().dynamicPort().extensions(new GistApiTransformer()));
        server.start();
        server.stubFor(any(anyUrl()).willReturn(aResponse().withTransformers("gist-api")));
    }

    /** Stops the embedded server. Called automatically when used as a JUnit {@code @Rule}. */
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    public String baseUrl() {
        return server.baseUrl();
    }

    /** A {@link GistApiClient} wired to this fake, with the small + raw-HTTP content-recovery chain (no IDE). */
    public GistApiClient newApiClient() {
        HttpClient httpClient = GistApiClient.newHttpClient();
        return new GistApiClient(() -> TOKEN, this::baseUrl, httpClient, "",
                DefaultGistFileContentProvider.create(null, httpClient, () -> TOKEN));
    }

    private static final class StoredGist {
        final String id;
        String description;
        String updatedAt;
        String etag;
        final Map<String, String> files = new LinkedHashMap<>();

        StoredGist(String id) {
            this.id = id;
        }
    }

    private final class GistApiTransformer implements ResponseDefinitionTransformerV2 {

        @Override
        public String getName() {
            return "gist-api";
        }

        @Override
        public boolean applyGlobally() {
            return false;
        }

        @Override
        public synchronized ResponseDefinition transform(ServeEvent serveEvent) {
            String method = serveEvent.getRequest().getMethod().getName();
            String url = serveEvent.getRequest().getUrl();
            String path = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
            String body = serveEvent.getRequest().getBodyAsString();
            String ifNoneMatch = serveEvent.getRequest().getHeader("If-None-Match");

            if (path.equals("/user") && method.equals("GET")) {
                return json(200, "{\"login\":\"test-user\",\"name\":\"Test User\"}", null);
            }
            if (path.equals("/gists")) {
                if (method.equals("POST")) {
                    return createGist(body);
                }
                if (method.equals("GET")) {
                    return listGists();
                }
            }
            if (path.startsWith("/gists/")) {
                String id = path.substring("/gists/".length());
                return switch (method) {
                    case "GET" -> loadGist(id, ifNoneMatch);
                    case "PATCH" -> updateGist(id, body);
                    case "DELETE" -> deleteGist(id);
                    default -> json(405, "{\"message\":\"Method Not Allowed\"}", null);
                };
            }
            if (path.startsWith("/raw/") && method.equals("GET")) {
                return rawContent(path);
            }
            return json(404, "{\"message\":\"Not Found\"}", null);
        }

        private ResponseDefinition createGist(String body) {
            JsonObject request = gson.fromJson(body, JsonObject.class);
            StoredGist gist = new StoredGist(UUID.randomUUID().toString().replace("-", ""));
            gist.description = request.has("description") && !request.get("description").isJsonNull()
                    ? request.get("description").getAsString() : null;
            putFiles(gist, request);
            touch(gist);
            gists.put(gist.id, gist);
            return json(201, gson.toJson(toJson(gist)), gist.etag);
        }

        private ResponseDefinition loadGist(String id, String ifNoneMatch) {
            StoredGist gist = gists.get(id);
            if (gist == null) {
                return json(404, "{\"message\":\"Not Found\"}", null);
            }
            if (gist.etag.equals(ifNoneMatch)) {
                return aResponse().withStatus(304).withHeader("ETag", gist.etag).build();
            }
            return json(200, gson.toJson(toJson(gist)), gist.etag);
        }

        private ResponseDefinition updateGist(String id, String body) {
            StoredGist gist = gists.get(id);
            if (gist == null) {
                return json(404, "{\"message\":\"Not Found\"}", null);
            }
            putFiles(gist, gson.fromJson(body, JsonObject.class));
            touch(gist);
            return json(200, gson.toJson(toJson(gist)), gist.etag);
        }

        private ResponseDefinition deleteGist(String id) {
            if (gists.remove(id) == null) {
                return json(404, "{\"message\":\"Not Found\"}", null);
            }
            return aResponse().withStatus(204).build();
        }

        private ResponseDefinition listGists() {
            var array = new com.google.gson.JsonArray();
            for (StoredGist gist : gists.values()) {
                array.add(toJson(gist));
            }
            return json(200, gson.toJson(array), null);
        }

        private ResponseDefinition rawContent(String path) {
            // /raw/{id}/{filename}
            String rest = path.substring("/raw/".length());
            int slash = rest.indexOf('/');
            if (slash < 0) {
                return json(404, "Not Found", null);
            }
            StoredGist gist = gists.get(rest.substring(0, slash));
            String fileName = rest.substring(slash + 1);
            if (gist == null || !gist.files.containsKey(fileName)) {
                return json(404, "Not Found", null);
            }
            return aResponse().withStatus(200)
                    .withHeader("Content-Type", "text/plain; charset=utf-8")
                    .withBody(gist.files.get(fileName))
                    .build();
        }

        private void putFiles(StoredGist gist, JsonObject request) {
            if (!request.has("files") || request.get("files").isJsonNull()) {
                return;
            }
            JsonObject files = request.getAsJsonObject("files");
            for (String fileName : files.keySet()) {
                gist.files.put(fileName, files.getAsJsonObject(fileName).get("content").getAsString());
            }
        }

        private void touch(StoredGist gist) {
            gist.updatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
            gist.etag = "\"v" + versionSeq.incrementAndGet() + "\"";
        }

        private JsonObject toJson(StoredGist gist) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", gist.id);
            obj.addProperty("description", gist.description);
            obj.addProperty("updated_at", gist.updatedAt);
            obj.addProperty("html_url", baseUrl() + "/" + gist.id);
            obj.addProperty("git_pull_url", baseUrl() + "/" + gist.id + ".git");
            JsonObject owner = new JsonObject();
            owner.addProperty("login", "test-user");
            obj.add("owner", owner);
            JsonObject files = new JsonObject();
            for (Map.Entry<String, String> entry : gist.files.entrySet()) {
                String content = entry.getValue();
                JsonObject file = new JsonObject();
                file.addProperty("filename", entry.getKey());
                file.addProperty("raw_url", baseUrl() + "/raw/" + gist.id + "/" + entry.getKey());
                if (content.length() > TRUNCATE_THRESHOLD) {
                    file.addProperty("truncated", true);
                    file.addProperty("content", content.substring(0, TRUNCATE_THRESHOLD));
                } else {
                    file.addProperty("truncated", false);
                    file.addProperty("content", content);
                }
                files.add(entry.getKey(), file);
            }
            obj.add("files", files);
            return obj;
        }

        private ResponseDefinition json(int status, String body, String etag) {
            var builder = aResponse().withStatus(status)
                    .withHeader("Content-Type", "application/json")
                    .withBody(body);
            if (etag != null) {
                builder = builder.withHeader("ETag", etag);
            }
            return builder.build();
        }
    }
}
