package mesfavoris.github.operations;

import mesfavoris.remote.ConflictException;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class UpdateGistOperationTest {
    private GistApiClient mockApiClient;
    private UpdateGistOperation operation;

    @Before
    public void setUp() {
        mockApiClient = mock(GistApiClient.class);
        operation = new UpdateGistOperation(mockApiClient);
    }

    @Test
    public void testUpdateGist_success() throws Exception {
        GistApiClient.GistResponse expectedResponse = new GistApiClient.GistResponse();
        expectedResponse.updated_at = "2024-01-02T00:00:00Z";
        when(mockApiClient.updateGist(anyString(), anyString(), anyString(), anyString())).thenReturn(expectedResponse);
        byte[] content = "{}".getBytes(StandardCharsets.UTF_8);

        GistApiClient.GistResponse result = operation.updateGist("abc123", content,
                "2024-01-01T00:00:00Z", null);

        assertThat(result.updated_at).isEqualTo("2024-01-02T00:00:00Z");
        verify(mockApiClient).updateGist(eq("abc123"), eq("bookmarks.json"), eq("{}"), eq("2024-01-01T00:00:00Z"));
    }

    @Test
    public void testUpdateGist_propagatesConflictException() throws Exception {
        when(mockApiClient.updateGist(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new ConflictException());

        Throwable thrown = catchThrowable(() ->
                operation.updateGist("abc123", "{}".getBytes(StandardCharsets.UTF_8),
                        "2024-01-01T00:00:00Z", null));

        assertThat(thrown).isInstanceOf(ConflictException.class);
    }

    @Test
    public void testUpdateGist_withNullEtag_passesNullToApiClient() throws Exception {
        GistApiClient.GistResponse expectedResponse = new GistApiClient.GistResponse();
        when(mockApiClient.updateGist(anyString(), anyString(), anyString(), isNull())).thenReturn(expectedResponse);

        operation.updateGist("abc123", "{}".getBytes(StandardCharsets.UTF_8), null, null);

        verify(mockApiClient).updateGist("abc123", "bookmarks.json", "{}", null);
    }
}
