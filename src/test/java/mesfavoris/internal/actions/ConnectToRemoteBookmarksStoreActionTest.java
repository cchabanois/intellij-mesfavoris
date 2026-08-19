package mesfavoris.internal.actions;

import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code errorReason} must never return null, so the user never sees "Failed to connect to ...: null". */
public class ConnectToRemoteBookmarksStoreActionTest {

    @Test
    public void testUsesExceptionMessageWhenPresent() {
        assertThat(ConnectToRemoteBookmarksStoreAction.errorReason(new IOException("Connection refused")))
                .isEqualTo("Connection refused");
    }

    @Test
    public void testFallsBackToClassNameWhenMessageIsNull() {
        // Bare ConnectException with no message is exactly what produced the "null" report.
        assertThat(ConnectToRemoteBookmarksStoreAction.errorReason(new ConnectException()))
                .isEqualTo("ConnectException");
    }

    @Test
    public void testFallsBackToCauseMessageWhenTopLevelMessageIsNull() {
        IOException e = new IOException((String) null, new ConnectException("Connection refused"));
        assertThat(ConnectToRemoteBookmarksStoreAction.errorReason(e))
                .isEqualTo("IOException: Connection refused");
    }
}
