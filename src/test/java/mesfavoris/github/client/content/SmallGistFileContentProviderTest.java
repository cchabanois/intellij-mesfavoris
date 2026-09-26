package mesfavoris.github.client.content;

import mesfavoris.github.client.GistFile;
import mesfavoris.github.client.GistResponse;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SmallGistFileContentProviderTest {

    private final SmallGistFileContentProvider provider = new SmallGistFileContentProvider();

    @Test
    public void testReturnsInlineContent_whenNotTruncated() throws Exception {
        GistFile file = file(false, "{\"version\":\"1.0\"}");

        byte[] content = provider.getFileContent(new GistResponse(), file);

        assertThat(new String(content, StandardCharsets.UTF_8)).isEqualTo("{\"version\":\"1.0\"}");
    }

    @Test
    public void testThrows_whenTruncated() {
        GistFile file = file(true, "{\"parti");

        assertThatThrownBy(() -> provider.getFileContent(new GistResponse(), file))
                .isInstanceOf(java.io.IOException.class);
    }

    @Test
    public void testThrows_whenContentIsNull() {
        GistFile file = file(false, null);

        assertThatThrownBy(() -> provider.getFileContent(new GistResponse(), file))
                .isInstanceOf(java.io.IOException.class);
    }

    private static GistFile file(boolean truncated, String content) {
        GistFile file = new GistFile();
        file.filename = "bookmarks.json";
        file.truncated = truncated;
        file.content = content;
        return file;
    }
}
