package mesfavoris.github.client;

import mesfavoris.remote.ConflictException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * GitHub Gist REST API operations. Extracted from {@link GistApiClient} so callers can depend on this
 * abstraction and be unit-tested with a mock.
 */
public interface IGistApiClient {

    GistResponse createGist(String description, String fileName, String content) throws IOException;

    GistResponse updateGist(String gistId, String fileName, String content,
                            @Nullable String expectedUpdatedAt) throws IOException, ConflictException;

    GistResponse loadGist(String gistId) throws IOException;

    void deleteGist(String gistId) throws IOException;

    @Nullable
    String conditionalGetEtag(String gistId, @Nullable String ifNoneMatch) throws IOException;

    List<GistResponse> listGists() throws IOException;

    UserResponse getAuthenticatedUser() throws IOException;
}
