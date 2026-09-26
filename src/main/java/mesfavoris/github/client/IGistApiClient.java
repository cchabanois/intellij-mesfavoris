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

    /** The OAuth token used to authenticate requests; also needed to authenticate git clones of a Gist. */
    String getToken();

    GistResponse createGist(String description, String fileName, String content) throws IOException;

    GistResponse updateGist(String gistId, String fileName, String content,
                            @Nullable String expectedUpdatedAt) throws IOException, ConflictException;

    GistResponse loadGist(String gistId) throws IOException;

    /**
     * Returns the full content of {@code file}, transparently recovering it (raw HTTP fetch, or git clone)
     * when the API truncated it. Backed by the {@link IGistFileContentProvider} the client was created with.
     */
    byte[] getFileContent(GistResponse gist, GistFile file) throws IOException;

    void deleteGist(String gistId) throws IOException;

    @Nullable
    String conditionalGetEtag(String gistId, @Nullable String ifNoneMatch) throws IOException;

    List<GistResponse> listGists() throws IOException;

    UserResponse getAuthenticatedUser() throws IOException;
}
