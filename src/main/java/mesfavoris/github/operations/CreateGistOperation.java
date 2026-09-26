package mesfavoris.github.operations;
import mesfavoris.github.client.IGistApiClient;
import mesfavoris.github.client.GistResponse;

import com.intellij.openapi.progress.ProgressIndicator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Creates a new private Gist to back a bookmark folder; the description is prefixed with {@code "mesfavoris: "}. */
public class CreateGistOperation {
    private final IGistApiClient apiClient;

    public CreateGistOperation(IGistApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public GistResponse createGist(String bookmarkFolderName, byte[] content,
                                                  ProgressIndicator indicator) throws IOException {
        if (indicator != null) {
            indicator.setText("Creating GitHub Gist for bookmark folder");
        }
        String description = "mesfavoris: " + bookmarkFolderName;
        return apiClient.createGist(description, LoadGistOperation.BOOKMARKS_FILE_NAME,
                new String(content, StandardCharsets.UTF_8));
    }
}
