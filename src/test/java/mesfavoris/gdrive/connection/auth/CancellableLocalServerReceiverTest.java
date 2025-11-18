package mesfavoris.gdrive.connection.auth;

import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class CancellableLocalServerReceiverTest {

    private VerificationCodeReceiver mockReceiver;
    private ProgressIndicator progressIndicator;
    private CancellableLocalServerReceiver cancellableReceiver;

    @Before
    public void setUp() {
        mockReceiver = mock(VerificationCodeReceiver.class);
        progressIndicator = new ProgressIndicatorBase();
        cancellableReceiver = new CancellableLocalServerReceiver(mockReceiver, progressIndicator);
    }

    @After
    public void tearDown() throws Exception {
        if (cancellableReceiver != null) {
            try {
                cancellableReceiver.stop();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void testWaitForCodeReturnsCodeWhenNotCancelled() throws Exception {
        // Given
        String expectedCode = "test-auth-code-123";
        when(mockReceiver.waitForCode()).thenReturn(expectedCode);

        // When
        String actualCode = cancellableReceiver.waitForCode();

        // Then
        assertThat(actualCode).isEqualTo(expectedCode);
        verify(mockReceiver).waitForCode();
    }

    @Test
    public void testWaitForCodeThrowsExceptionWhenCancelled() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(1);

        // Mock receiver that waits until cancelled
        when(mockReceiver.waitForCode()).thenAnswer(invocation -> {
            latch.await(10, TimeUnit.SECONDS);
            return "should-not-return";
        });

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(mockReceiver).stop();

        // Cancel the progress indicator to trigger cancellation
        progressIndicator.cancel();

        // When/Then
        assertThatThrownBy(() -> cancellableReceiver.waitForCode())
                .isInstanceOf(IOException.class)
                .hasMessageContaining("cancelled by user");

        // Verify stop was called
        verify(mockReceiver).stop();
    }

    @Test
    public void testWaitForCodePropagatesIOException() throws Exception {
        // Given
        IOException expectedException = new IOException("Connection failed");
        when(mockReceiver.waitForCode()).thenThrow(expectedException);

        // When/Then
        assertThatThrownBy(() -> cancellableReceiver.waitForCode())
                .isInstanceOf(IOException.class)
                .hasMessage("Connection failed");
    }

    @Test
    public void testGetRedirectUriDelegatesToReceiver() throws Exception {
        // Given
        String expectedUri = "http://localhost:8080/callback";
        when(mockReceiver.getRedirectUri()).thenReturn(expectedUri);

        // When
        String actualUri = cancellableReceiver.getRedirectUri();

        // Then
        assertThat(actualUri).isEqualTo(expectedUri);
        verify(mockReceiver).getRedirectUri();
    }

    @Test
    public void testStopDelegatesToReceiver() throws Exception {
        // When
        cancellableReceiver.stop();

        // Then
        verify(mockReceiver).stop();
    }

}

