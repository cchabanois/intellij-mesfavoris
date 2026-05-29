package mesfavoris.github.operations;

import com.intellij.openapi.progress.ProgressIndicator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Loads a Gist's {@code bookmarks.json} content, fetching raw content if the file is truncated. */
public class LoadGistOperation {
    static final String BOOKMARKS_FILE_NAME = "bookmarks.json";

    private final GistApiClient apiClient;

    public LoadGistOperation(GistApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public GistContents loadGist(String gistId, ProgressIndicator indicator) throws IOException {
        if (indicator != null) {
            indicator.setText("Loading bookmark folder from GitHub Gist");
        }
        GistApiClient.GistResponse response = apiClient.loadGist(gistId);
        GistApiClient.GistFile file = response.files != null ? response.files.get(BOOKMARKS_FILE_NAME) : null;
        if (file == null || file.content == null) {
            throw new IOException("Gist " + gistId + " does not contain " + BOOKMARKS_FILE_NAME);
        }
        if (Boolean.TRUE.equals(file.truncated) && file.raw_url != null) {
            file.content = apiClient.fetchRawContent(file.raw_url);
        }
        byte[] content = file.content.getBytes(StandardCharsets.UTF_8);
        return new GistContents(content, response.updated_at, response);
    }

    /** The etag field carries {@code updated_at} (ISO-8601), used as an optimistic-lock token for saves. */
    public record GistContents(byte[] content, String etag, GistApiClient.GistResponse response) {
    }
}
