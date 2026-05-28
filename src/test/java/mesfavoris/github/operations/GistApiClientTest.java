package mesfavoris.github.operations;

import mesfavoris.remote.ConflictException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GistApiClientTest {
    private HttpClient mockHttpClient;
    private GistApiClient apiClient;

    @Before
    public void setUp() {
        mockHttpClient = mock(HttpClient.class);
        apiClient = new GistApiClient(() -> "test-token", mockHttpClient);
    }

    // --- createGist ---

    @Test
    public void testCreateGist_success() throws Exception {
        String responseBody = """
                {
                  "id": "abc123",
                  "updated_at": "2024-01-01T00:00:00Z",
                  "html_url": "https://gist.github.com/abc123",
                  "owner": { "login": "cchabanois" },
                  "files": { "bookmarks.json": { "content": "{}" } }
                }
                """;
        givenResponse(201, responseBody);

        GistApiClient.GistResponse response = apiClient.createGist("test gist", "bookmarks.json", "{}");

        assertThat(response.id).isEqualTo("abc123");
        assertThat(response.updated_at).isEqualTo("2024-01-01T00:00:00Z");
        assertThat(response.html_url).isEqualTo("https://gist.github.com/abc123");
        assertThat(response.owner.login).isEqualTo("cchabanois");
        verifyRequestMethod("POST");
    }

    @Test
    public void testCreateGist_failure() throws Exception {
        givenResponse(422, "{\"message\":\"Validation Failed\"}");

        Throwable thrown = catchThrowable(() -> apiClient.createGist("test gist", "bookmarks.json", "{}"));

        assertThat(thrown).isInstanceOf(IOException.class)
                .hasMessageContaining("422");
    }

    // --- updateGist ---

    @Test
    public void testUpdateGist_conflict_whenUpdatedAtDiffers() throws Exception {
        String fetchBody = """
                {
                  "id": "abc123",
                  "updated_at": "2024-01-02T00:00:00Z",
                  "files": { "bookmarks.json": { "content": "{}" } }
                }
                """;
        givenResponse(200, fetchBody);

        Throwable thrown = catchThrowable(() ->
                apiClient.updateGist("abc123", "bookmarks.json", "{}", "2024-01-01T00:00:00Z"));

        assertThat(thrown).isInstanceOf(ConflictException.class);
    }

    @Test
    public void testUpdateGist_success_whenUpdatedAtMatches() throws Exception {
        String sameUpdatedAt = "2024-01-01T00:00:00Z";
        String fetchBody = """
                {
                  "id": "abc123",
                  "updated_at": "2024-01-01T00:00:00Z",
                  "files": { "bookmarks.json": { "content": "{}" } }
                }
                """;
        String patchBody = """
                {
                  "id": "abc123",
                  "updated_at": "2024-01-03T00:00:00Z",
                  "html_url": "https://gist.github.com/abc123",
                  "files": { "bookmarks.json": { "content": "{new:true}" } }
                }
                """;
        HttpResponse<String> fetchResponse = mockResponse(200, fetchBody);
        HttpResponse<String> patchResponse = mockResponse(200, patchBody);
        doReturn(fetchResponse).doReturn(patchResponse).when(mockHttpClient).send(any(), any());

        GistApiClient.GistResponse result = apiClient.updateGist("abc123", "bookmarks.json", "{new:true}", sameUpdatedAt);

        assertThat(result.updated_at).isEqualTo("2024-01-03T00:00:00Z");
    }

    @Test
    public void testUpdateGist_noConflictCheck_whenEtagIsNull() throws Exception {
        String patchBody = """
                {
                  "id": "abc123",
                  "updated_at": "2024-01-03T00:00:00Z",
                  "html_url": "https://gist.github.com/abc123",
                  "files": { "bookmarks.json": { "content": "{}" } }
                }
                """;
        givenResponse(200, patchBody);

        GistApiClient.GistResponse result = apiClient.updateGist("abc123", "bookmarks.json", "{}", null);

        assertThat(result.updated_at).isEqualTo("2024-01-03T00:00:00Z");
        // Only one call (no GET for conflict check)
        verify(mockHttpClient, times(1)).send(any(), any());
    }

    // --- loadGist ---

    @Test
    public void testLoadGist_success() throws Exception {
        String responseBody = """
                {
                  "id": "abc123",
                  "updated_at": "2024-01-01T00:00:00Z",
                  "html_url": "https://gist.github.com/abc123",
                  "owner": { "login": "cchabanois" },
                  "files": { "bookmarks.json": { "content": "{\\"version\\":\\"1.0\\"}", "truncated": false } }
                }
                """;
        givenResponse(200, responseBody);

        GistApiClient.GistResponse response = apiClient.loadGist("abc123");

        assertThat(response.id).isEqualTo("abc123");
        assertThat(response.files.get("bookmarks.json").content).isEqualTo("{\"version\":\"1.0\"}");
    }

    @Test
    public void testLoadGist_truncated_returnsRawResponseAsIs() throws Exception {
        // Truncation is handled by LoadGistOperation, not by GistApiClient
        String responseBody = """
                {
                  "id": "abc123",
                  "updated_at": "2024-01-01T00:00:00Z",
                  "files": {
                    "bookmarks.json": {
                      "content": "",
                      "raw_url": "https://gist.githubusercontent.com/raw/abc123/bookmarks.json",
                      "truncated": true
                    }
                  }
                }
                """;
        givenResponse(200, responseBody);

        GistApiClient.GistResponse response = apiClient.loadGist("abc123");

        assertThat(response.files.get("bookmarks.json").truncated).isTrue();
        assertThat(response.files.get("bookmarks.json").raw_url).isNotNull();
        verify(mockHttpClient, times(1)).send(any(), any());
    }

    @Test
    public void testLoadGist_notFound() throws Exception {
        givenResponse(404, "{\"message\":\"Not Found\"}");

        Throwable thrown = catchThrowable(() -> apiClient.loadGist("unknown"));

        assertThat(thrown).isInstanceOf(IOException.class)
                .hasMessageContaining("404");
    }

    // --- deleteGist ---

    @Test
    public void testDeleteGist_success() throws Exception {
        givenResponse(204, "");

        apiClient.deleteGist("abc123");

        verify(mockHttpClient, times(1)).send(any(), any());
    }

    @Test
    public void testDeleteGist_notFound_treatedAsSuccess() throws Exception {
        givenResponse(404, "{\"message\":\"Not Found\"}");

        // Should not throw
        apiClient.deleteGist("abc123");
    }

    @Test
    public void testDeleteGist_serverError_throws() throws Exception {
        givenResponse(500, "{\"message\":\"Internal Server Error\"}");

        Throwable thrown = catchThrowable(() -> apiClient.deleteGist("abc123"));

        assertThat(thrown).isInstanceOf(IOException.class)
                .hasMessageContaining("500");
    }

    // --- conditionalGetEtag ---

    @Test
    public void testConditionalGetEtag_notModified_returnsNull() throws Exception {
        givenResponse(304, "");

        String etag = apiClient.conditionalGetEtag("abc123", "\"some-etag\"");

        assertThat(etag).isNull();
    }

    @Test
    public void testConditionalGetEtag_modified_returnsNewEtag() throws Exception {
        HttpResponse<String> response = mockResponse(200, "{\"id\":\"abc123\",\"updated_at\":\"2024-01-02T00:00:00Z\",\"files\":{}}");
        when(response.headers()).thenReturn(
                HttpHeaders.of(Map.of("ETag", List.of("\"new-etag\"")), (a, b) -> true));
        doReturn(response).when(mockHttpClient).send(any(), any());

        String etag = apiClient.conditionalGetEtag("abc123", "\"old-etag\"");

        assertThat(etag).isEqualTo("\"new-etag\"");
    }

    @Test
    public void testConditionalGetEtag_noStoredEtag_sendsNoIfNoneMatchHeader() throws Exception {
        HttpResponse<String> response = mockResponse(200, "{\"id\":\"abc123\",\"updated_at\":\"2024-01-01T00:00:00Z\",\"files\":{}}");
        when(response.headers()).thenReturn(
                HttpHeaders.of(Map.of("ETag", List.of("\"new-etag\"")), (a, b) -> true));
        doReturn(response).when(mockHttpClient).send(any(), any());

        String etag = apiClient.conditionalGetEtag("abc123", null);

        assertThat(etag).isEqualTo("\"new-etag\"");
    }

    // --- getAuthenticatedUser ---

    @Test
    public void testGetAuthenticatedUser_success() throws Exception {
        givenResponse(200, "{\"login\":\"cchabanois\",\"name\":\"Cedric\"}");

        GistApiClient.UserResponse user = apiClient.getAuthenticatedUser();

        assertThat(user.login).isEqualTo("cchabanois");
        assertThat(user.name).isEqualTo("Cedric");
    }

    @Test
    public void testGetAuthenticatedUser_invalidToken_throwsWithMessage() throws Exception {
        givenResponse(401, "{\"message\":\"Bad credentials\"}");

        Throwable thrown = catchThrowable(() -> apiClient.getAuthenticatedUser());

        assertThat(thrown).isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid GitHub token");
    }

    // --- Helpers ---

    private void givenResponse(int statusCode, String body) throws Exception {
        HttpResponse<String> response = mockResponse(statusCode, body);
        doReturn(response).when(mockHttpClient).send(any(), any());
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(int statusCode, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));
        return response;
    }

    private void verifyRequestMethod(String expectedMethod) throws Exception {
        verify(mockHttpClient).send(
                argThat(req -> req.method().equals(expectedMethod)),
                any());
    }
}
