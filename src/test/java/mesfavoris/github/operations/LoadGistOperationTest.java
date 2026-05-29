package mesfavoris.github.operations;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

public class LoadGistOperationTest {
    private GistApiClient mockApiClient;
    private LoadGistOperation operation;

    @Before
    public void setUp() {
        mockApiClient = mock(GistApiClient.class);
        operation = new LoadGistOperation(mockApiClient);
    }

    @Test
    public void testLoadGist_success() throws Exception {
        GistApiClient.GistResponse response = buildResponse("abc123", "2024-01-01T00:00:00Z",
                "{\"version\":\"1.0\"}");
        when(mockApiClient.loadGist("abc123")).thenReturn(response);

        LoadGistOperation.GistContents result = operation.loadGist("abc123", null);

        assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo("{\"version\":\"1.0\"}");
        assertThat(result.etag()).isEqualTo("2024-01-01T00:00:00Z");
        assertThat(result.response()).isSameAs(response);
    }

    @Test
    public void testLoadGist_missingBookmarksJson_throwsIOException() throws Exception {
        GistApiClient.GistResponse response = new GistApiClient.GistResponse();
        response.id = "abc123";
        response.files = Map.of();
        when(mockApiClient.loadGist("abc123")).thenReturn(response);

        Throwable thrown = catchThrowable(() -> operation.loadGist("abc123", null));

        assertThat(thrown).isInstanceOf(IOException.class)
                .hasMessageContaining("abc123");
    }

    @Test
    public void testLoadGist_nullContent_throwsIOException() throws Exception {
        GistApiClient.GistResponse response = new GistApiClient.GistResponse();
        response.id = "abc123";
        GistApiClient.GistFile file = new GistApiClient.GistFile();
        file.content = null;
        response.files = Map.of("bookmarks.json", file);
        when(mockApiClient.loadGist("abc123")).thenReturn(response);

        Throwable thrown = catchThrowable(() -> operation.loadGist("abc123", null));

        assertThat(thrown).isInstanceOf(IOException.class);
    }

    private GistApiClient.GistResponse buildResponse(String id, String updatedAt, String content) {
        GistApiClient.GistResponse response = new GistApiClient.GistResponse();
        response.id = id;
        response.updated_at = updatedAt;
        GistApiClient.GistFile file = new GistApiClient.GistFile();
        file.content = content;
        response.files = Map.of("bookmarks.json", file);
        return response;
    }
}
