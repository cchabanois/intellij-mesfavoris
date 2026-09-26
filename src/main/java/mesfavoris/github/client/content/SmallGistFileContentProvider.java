package mesfavoris.github.client.content;
import mesfavoris.github.client.IGistFileContentProvider;

import mesfavoris.github.client.GistFile;
import mesfavoris.github.client.GistResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Returns the content already present in the API response. Applicable only to non-truncated files (files
 * up to 1&nbsp;MB); for truncated files the {@code content} field holds only a prefix, so this provider
 * throws to let the next provider fetch the full content.
 */
public class SmallGistFileContentProvider implements IGistFileContentProvider {

    @Override
    public byte[] getFileContent(GistResponse gist, GistFile file) throws IOException {
        if (Boolean.TRUE.equals(file.truncated)) {
            throw new IOException("Gist file %s is truncated".formatted(file.filename));
        }
        if (file.content == null) {
            throw new IOException("Gist file %s has no content".formatted(file.filename));
        }
        return file.content.getBytes(StandardCharsets.UTF_8);
    }
}
