package mesfavoris.github.operations;

import com.intellij.openapi.progress.ProgressIndicator;

import java.io.IOException;

/** Deletes a Gist; a 404 response is treated as success (idempotent). */
public class DeleteGistOperation {
    private final GistApiClient apiClient;

    public DeleteGistOperation(GistApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void deleteGist(String gistId, ProgressIndicator indicator) throws IOException {
        if (indicator != null) {
            indicator.setText("Deleting GitHub Gist");
        }
        apiClient.deleteGist(gistId);
    }
}
