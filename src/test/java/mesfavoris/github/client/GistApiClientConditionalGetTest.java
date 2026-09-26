package mesfavoris.github.client;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Change polling must use HEAD so it never downloads the gist content. */
public class GistApiClientConditionalGetTest {

    @Test
    public void testConditionalGetEtag_sendsHeadWithIfNoneMatch() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        doReturn(response(304, Map.of())).when(httpClient).send(any(), any());

        String etag = client(httpClient).conditionalGetEtag("abc", "\"v1\"");

        assertThat(etag).isNull();
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(request.capture(), any());
        assertThat(request.getValue().method()).isEqualTo("HEAD");
        assertThat(request.getValue().uri().getPath()).isEqualTo("/gists/abc");
        assertThat(request.getValue().headers().firstValue("If-None-Match")).contains("\"v1\"");
    }

    @Test
    public void testConditionalGetEtag_modified_returnsNewEtag() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        doReturn(response(200, Map.of("ETag", List.of("\"v2\"")))).when(httpClient).send(any(), any());

        assertThat(client(httpClient).conditionalGetEtag("abc", "\"v1\"")).isEqualTo("\"v2\"");
    }

    private static GistApiClient client(HttpClient httpClient) {
        return new GistApiClient(() -> "token", () -> "https://api.github.com", httpClient);
    }

    private static HttpResponse<String> response(int status, Map<String, List<String>> headers) {
        HttpResponse<String> response = mock();
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn("");
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (k, v) -> true));
        return response;
    }
}
