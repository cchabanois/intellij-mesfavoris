package mesfavoris.github.client.content;

import mesfavoris.github.client.GistFile;
import mesfavoris.github.client.GistResponse;
import org.junit.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests (no network) for {@link HttpGetGistFileContentProvider}: it accepts the raw content only
 * when it starts with the truncated prefix the API already returned, and throws otherwise so the chain falls
 * back to a git clone. The {@link HttpClient} is mocked to avoid real HTTP.
 */
public class HttpGetGistFileContentProviderTest {

    // Partial content returned by the API for a truncated file (longer than the match margin).
    private static final String TRUNCATED_PREFIX = "{\"bookmarks\":[{\"id\":\"1\",\"name\":\"first\"},{\"id\":\"2\"";
    private static final String FULL_CONTENT = TRUNCATED_PREFIX + ",\"name\":\"second\"}]}";
    private static final String RAW_URL = "https://gist.githubusercontent.com/u/abc/raw/bookmarks.json";

    @Test
    public void testReturnsRaw_whenItExtendsTruncatedPrefix() throws Exception {
        HttpGetGistFileContentProvider provider = provider(httpClientReturning(200, FULL_CONTENT));

        byte[] content = provider.getFileContent(new GistResponse(), truncatedFile(RAW_URL));

        assertThat(new String(content, StandardCharsets.UTF_8)).isEqualTo(FULL_CONTENT);
    }

    @Test
    public void testThrows_whenRawIsLoginPage() throws Exception {
        HttpGetGistFileContentProvider provider =
                provider(httpClientReturning(200, "<!DOCTYPE html><html>Sign in to GitHub</html>"));

        assertThatThrownBy(() -> provider.getFileContent(new GistResponse(), truncatedFile(RAW_URL)))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void testThrows_whenNoRawUrl() {
        HttpGetGistFileContentProvider provider = provider(mock(HttpClient.class));

        assertThatThrownBy(() -> provider.getFileContent(new GistResponse(), truncatedFile(null)))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void testThrows_whenHttpStatusNot200() throws Exception {
        HttpGetGistFileContentProvider provider = provider(httpClientReturning(404, ""));

        assertThatThrownBy(() -> provider.getFileContent(new GistResponse(), truncatedFile(RAW_URL)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("404");
    }

    private static HttpGetGistFileContentProvider provider(HttpClient httpClient) {
        return new HttpGetGistFileContentProvider(httpClient, () -> "token");
    }

    private static HttpClient httpClientReturning(int status, String body) throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock();
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        doReturn(response).when(httpClient).send(any(), any());
        return httpClient;
    }

    private static GistFile truncatedFile(String rawUrl) {
        GistFile file = new GistFile();
        file.filename = "bookmarks.json";
        file.truncated = true;
        file.content = TRUNCATED_PREFIX;
        file.raw_url = rawUrl;
        return file;
    }
}
