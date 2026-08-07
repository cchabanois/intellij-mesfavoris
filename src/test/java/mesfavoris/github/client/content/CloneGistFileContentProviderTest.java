package mesfavoris.github.client.content;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for {@link CloneGistFileContentProvider#authenticatedGitUrl}. The clone itself goes through
 * git4idea's {@code Git} service and is not exercised here since it needs a running IDE.
 */
public class CloneGistFileContentProviderTest {

    @Test
    public void testAuthenticatedGitUrl_embedsTokenAsUserInfo() throws Exception {
        String url = CloneGistFileContentProvider.authenticatedGitUrl(
                "https://gist.github.com/abc123.git", "ghp_secret");

        assertThat(url).isEqualTo("https://x-access-token:ghp_secret@gist.github.com/abc123.git");
    }

    @Test
    public void testAuthenticatedGitUrl_preservesEnterpriseHostAndPath() throws Exception {
        String url = CloneGistFileContentProvider.authenticatedGitUrl(
                "https://github.example.com/gist/abc123.git", "tok");

        assertThat(url).isEqualTo("https://x-access-token:tok@github.example.com/gist/abc123.git");
    }
}
