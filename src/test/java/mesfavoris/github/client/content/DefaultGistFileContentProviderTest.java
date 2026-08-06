package mesfavoris.github.client.content;
import mesfavoris.github.client.IGistFileContentProvider;

import mesfavoris.github.client.GistFile;
import mesfavoris.github.client.GistResponse;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.fail;

/**
 * Pure unit tests for the {@link DefaultGistFileContentProvider} exception chain: it returns the content of
 * the first provider that succeeds and skips the ones that throw.
 */
public class DefaultGistFileContentProviderTest {

    private static final GistResponse GIST = new GistResponse();
    private static final GistFile FILE = new GistFile();

    @Test
    public void testReturnsFirstSuccess_andSkipsLaterProviders() throws Exception {
        IGistFileContentProvider first = (gist, file) -> "first".getBytes(StandardCharsets.UTF_8);
        IGistFileContentProvider second = (gist, file) -> fail("must not reach the second provider");
        DefaultGistFileContentProvider provider =
                new DefaultGistFileContentProvider(List.of(first, second));

        byte[] content = provider.getFileContent(GIST, FILE);

        assertThat(new String(content, StandardCharsets.UTF_8)).isEqualTo("first");
    }

    @Test
    public void testFallsThrough_toFirstSucceedingProvider() throws Exception {
        IGistFileContentProvider small = (gist, file) -> {
            throw new IOException("truncated");
        };
        IGistFileContentProvider http = (gist, file) -> {
            throw new IOException("login page");
        };
        IGistFileContentProvider clone = (gist, file) -> "cloned".getBytes(StandardCharsets.UTF_8);
        DefaultGistFileContentProvider provider =
                new DefaultGistFileContentProvider(List.of(small, http, clone));

        byte[] content = provider.getFileContent(GIST, FILE);

        assertThat(new String(content, StandardCharsets.UTF_8)).isEqualTo("cloned");
    }

    @Test
    public void testRethrowsLastException_whenAllProvidersFail() {
        IGistFileContentProvider a = (gist, file) -> {
            throw new IOException("first failure");
        };
        IGistFileContentProvider b = (gist, file) -> {
            throw new IOException("last failure");
        };
        DefaultGistFileContentProvider provider = new DefaultGistFileContentProvider(List.of(a, b));

        Throwable thrown = catchThrowable(() -> provider.getFileContent(GIST, FILE));

        assertThat(thrown).isInstanceOf(IOException.class).hasMessage("last failure");
    }
}
