package mesfavoris.github.operations;

import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.github.client.GistFile;
import mesfavoris.github.client.GistResponse;
import mesfavoris.github.client.IGistApiClient;

import java.io.IOException;

/** Loads a Gist's {@code bookmarks.json} content, recovering the full content when the file is truncated. */
public class LoadGistOperation {
    public static final String BOOKMARKS_FILE_NAME = "bookmarks.json";

    private final IGistApiClient apiClient;

    public LoadGistOperation(IGistApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public GistContents loadGist(String gistId, ProgressIndicator indicator) throws IOException {
        if (indicator != null) {
            indicator.setText("Loading bookmark folder from GitHub Gist");
        }
        GistResponse response = apiClient.loadGist(gistId);
        GistFile file = response.files != null ? response.files.get(BOOKMARKS_FILE_NAME) : null;
        if (file == null) {
            throw new IOException("Gist " + gistId + " does not contain " + BOOKMARKS_FILE_NAME);
        }
        byte[] content = apiClient.getFileContent(response, file);
        return new GistContents(content, response.updated_at, response);
    }

    /** The etag field carries {@code updated_at} (ISO-8601), used as an optimistic-lock token for saves. */
    public record GistContents(byte[] content, String etag, GistResponse response) {
    }
}
