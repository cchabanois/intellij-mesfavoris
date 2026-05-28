package mesfavoris.github.operations;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class CreateGistOperationTest {
    private GistApiClient mockApiClient;
    private CreateGistOperation operation;

    @Before
    public void setUp() {
        mockApiClient = mock(GistApiClient.class);
        operation = new CreateGistOperation(mockApiClient);
    }

    @Test
    public void testCreateGist_usesBookmarkFolderNameAsDescription() throws Exception {
        GistApiClient.GistResponse expectedResponse = new GistApiClient.GistResponse();
        expectedResponse.id = "abc123";
        expectedResponse.updated_at = "2024-01-01T00:00:00Z";
        when(mockApiClient.createGist(anyString(), anyString(), anyString())).thenReturn(expectedResponse);

        GistApiClient.GistResponse result = operation.createGist("My Bookmarks",
                "{}".getBytes(StandardCharsets.UTF_8), null);

        assertThat(result.id).isEqualTo("abc123");
        verify(mockApiClient).createGist(eq("mesfavoris: My Bookmarks"), eq("bookmarks.json"), eq("{}"));
    }

    @Test
    public void testCreateGist_encodesContentAsUtf8() throws Exception {
        GistApiClient.GistResponse expectedResponse = new GistApiClient.GistResponse();
        when(mockApiClient.createGist(anyString(), anyString(), anyString())).thenReturn(expectedResponse);
        byte[] content = "café".getBytes(StandardCharsets.UTF_8);

        operation.createGist("folder", content, null);

        verify(mockApiClient).createGist(anyString(), anyString(), eq("café"));
    }
}
