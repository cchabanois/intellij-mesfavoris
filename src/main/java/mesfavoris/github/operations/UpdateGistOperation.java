package mesfavoris.github.operations;

import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.remote.ConflictException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Updates a Gist's {@code bookmarks.json}, throwing {@link mesfavoris.remote.ConflictException} if the remote was modified since {@code etag}. */
public class UpdateGistOperation {
    private final GistApiClient apiClient;

    public UpdateGistOperation(GistApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public GistApiClient.GistResponse updateGist(String gistId, byte[] content,
                                                  @Nullable String etag,
                                                  ProgressIndicator indicator)
            throws IOException, ConflictException {
        if (indicator != null) {
            indicator.setText("Saving bookmark folder to GitHub Gist");
        }
        return apiClient.updateGist(gistId, LoadGistOperation.BOOKMARKS_FILE_NAME,
                new String(content, StandardCharsets.UTF_8), etag);
    }
}
