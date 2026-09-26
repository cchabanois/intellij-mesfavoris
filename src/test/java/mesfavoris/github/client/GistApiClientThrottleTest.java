package mesfavoris.github.client;

import org.junit.Test;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests (no network) for {@link GistApiClient}'s write throttling: mutative requests are spaced by
 * at least one second, while reads are not delayed.
 */
public class GistApiClientThrottleTest {

    private GistApiClient client(HttpClient httpClient) {
        return new GistApiClient(() -> "token", () -> "https://api.github.com", httpClient);
    }

    @Test
    public void testBackToBackWrites_areSpacedByAtLeastOneSecond() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        doReturn(ok(201, "{}")).when(httpClient).send(any(), any());
        GistApiClient client = client(httpClient);

        long start = System.nanoTime();
        client.createGist("d", "f", "{}");
        client.createGist("d", "f", "{}");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isGreaterThanOrEqualTo(900);
    }

    @Test
    public void testReads_areNotThrottled() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        doReturn(ok(200, "{}")).when(httpClient).send(any(), any());
        GistApiClient client = client(httpClient);

        long start = System.nanoTime();
        client.loadGist("id");
        client.loadGist("id");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(500);
    }

    private static HttpResponse<String> ok(int status, String body) {
        HttpResponse<String> response = mock();
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
}
