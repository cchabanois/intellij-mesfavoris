package mesfavoris.github.operations;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class DeleteGistOperationTest {
    private GistApiClient mockApiClient;
    private DeleteGistOperation operation;

    @Before
    public void setUp() {
        mockApiClient = mock(GistApiClient.class);
        operation = new DeleteGistOperation(mockApiClient);
    }

    @Test
    public void testDeleteGist_delegatesToApiClient() throws Exception {
        operation.deleteGist("abc123", null);

        verify(mockApiClient).deleteGist("abc123");
    }

    @Test
    public void testDeleteGist_propagatesIOException() throws Exception {
        doThrow(new IOException("network error")).when(mockApiClient).deleteGist("abc123");

        Throwable thrown = catchThrowable(() -> operation.deleteGist("abc123", null));

        assertThat(thrown).isInstanceOf(IOException.class).hasMessage("network error");
    }
}
