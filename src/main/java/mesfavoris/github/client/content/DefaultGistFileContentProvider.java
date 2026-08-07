package mesfavoris.github.client.content;

import com.intellij.openapi.project.Project;
import mesfavoris.github.client.GistFile;
import mesfavoris.github.client.GistResponse;
import mesfavoris.github.client.IGistFileContentProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Tries a list of {@link IGistFileContentProvider}s in order and returns the content from the first one that
 * succeeds. Since a provider throws {@link IOException} both when it is not applicable and when it fails,
 * this yields a simple exception chain: small (inline content) → raw HTTP GET → git clone. If every provider
 * throws, the last exception is rethrown.
 */
public class DefaultGistFileContentProvider implements IGistFileContentProvider {

    private final List<IGistFileContentProvider> providers;

    public DefaultGistFileContentProvider(List<IGistFileContentProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    /**
     * Builds the standard chain. The git-clone provider is only added when a {@code project} is available
     * (it needs git4idea's IDE-scoped {@code Git} service).
     *
     * @param httpClient    used by the raw HTTP fetch; pass the same instance the API client uses to share it
     * @param tokenSupplier supplies the OAuth token used to authenticate the raw HTTP fetch and the git clone
     */
    public static DefaultGistFileContentProvider create(@Nullable Project project, HttpClient httpClient,
                                                        Supplier<String> tokenSupplier) {
        List<IGistFileContentProvider> providers = new ArrayList<>();
        providers.add(new SmallGistFileContentProvider());
        providers.add(new HttpGetGistFileContentProvider(httpClient, tokenSupplier));
        if (project != null) {
            providers.add(new CloneGistFileContentProvider(project, tokenSupplier));
        }
        return new DefaultGistFileContentProvider(providers);
    }

    @Override
    public byte[] getFileContent(GistResponse gist, GistFile file) throws IOException {
        IOException last = null;
        for (IGistFileContentProvider provider : providers) {
            try {
                return provider.getFileContent(gist, file);
            } catch (IOException e) {
                last = e;
            }
        }
        throw last != null ? last
                : new IOException("No content provider available for gist " + gist.id);
    }
}
