package mesfavoris.github.client;

import org.junit.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests (no network) for {@link GistApiClient}'s handling of GitHub rate-limit responses: it backs
 * off per {@code retry-after} / {@code x-ratelimit-reset} and retries, but does not retry a plain 403.
 */
public class GistApiClientRateLimitTest {

    private GistApiClient client(HttpClient httpClient) {
        return new GistApiClient(() -> "token", () -> "https://api.github.com", httpClient);
    }

    @Test
    public void testRetriesAfterRetryAfter_thenSucceeds() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        doReturn(retryAfter("1"), ok201()).when(httpClient).send(any(), any());

        GistResponse response = client(httpClient).createGist("d", "f", "{}");

        assertThat(response).isNotNull();
        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    public void testWaitsUntilReset_whenRemainingZero_thenSucceeds() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        doReturn(rateLimitReset(Instant.now().getEpochSecond() + 1), ok201())
                .when(httpClient).send(any(), any());

        GistResponse response = client(httpClient).createGist("d", "f", "{}");

        assertThat(response).isNotNull();
        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    public void testDoesNotRetryPlainForbidden() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        doReturn(response(403, "Bad credentials", Map.of())).when(httpClient).send(any(), any());

        assertThatThrownBy(() -> client(httpClient).createGist("d", "f", "{}"))
                .isInstanceOf(IOException.class);
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    public void testGivesUpAfterMaxRetries() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        doReturn(retryAfter("1")).when(httpClient).send(any(), any());

        assertThatThrownBy(() -> client(httpClient).createGist("d", "f", "{}"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("rate limit");
        // initial attempt + MAX_RATE_LIMIT_RETRIES (3) = 4 sends
        verify(httpClient, times(4)).send(any(), any());
    }

    private static HttpResponse<String> ok201() {
        return response(201, "{}", Map.of());
    }

    private static HttpResponse<String> retryAfter(String seconds) {
        return response(403, "", Map.of("retry-after", List.of(seconds)));
    }

    private static HttpResponse<String> rateLimitReset(long resetEpochSeconds) {
        return response(403, "", Map.of(
                "x-ratelimit-remaining", List.of("0"),
                "x-ratelimit-reset", List.of(Long.toString(resetEpochSeconds))));
    }

    private static HttpResponse<String> response(int status, String body, Map<String, List<String>> headers) {
        HttpResponse<String> response = mock();
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (k, v) -> true));
        return response;
    }
}
