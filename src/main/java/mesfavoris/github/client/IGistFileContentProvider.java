package mesfavoris.github.client;

import java.io.IOException;

/**
 * Strategy for obtaining the full content of a single Gist file. Different implementations use different
 * mechanisms (inline API content, raw HTTP GET, git clone), and can be chained so that one takes over when
 * another is not applicable or fails. An implementation throws {@link IOException} when it cannot serve the
 * given file <em>or</em> when it fails.
 */
public interface IGistFileContentProvider {
    byte[] getFileContent(GistResponse gist, GistFile file) throws IOException;
}
