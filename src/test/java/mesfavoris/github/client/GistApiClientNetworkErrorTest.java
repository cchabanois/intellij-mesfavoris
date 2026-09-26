package mesfavoris.github.client;

import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** {@link GistApiClient} must propagate network failures unchanged so callers can catch their concrete type. */
public class GistApiClientNetworkErrorTest {

    @Test
    public void testPropagatesNetworkExceptionsUnchanged() throws Exception {
        List<IOException> networkFailures = List.of(
                new ConnectException("Connection refused"),
                new UnknownHostException("api.github.com"),
                new HttpTimeoutException("request timed out"),
                new IOException() // some failures have a null message
        );

        for (IOException failure : networkFailures) {
            HttpClient httpClient = mock(HttpClient.class);
            when(httpClient.send(any(), any())).thenThrow(failure);
            GistApiClient client = new GistApiClient(() -> "token", () -> "https://api.github.com", httpClient);

            assertThatThrownBy(client::getAuthenticatedUser)
                    .as("network failure %s must propagate unchanged", failure.getClass().getSimpleName())
                    .isSameAs(failure);
        }
    }
}
