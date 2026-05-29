package mesfavoris.github.dialogs;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AddGistLinkDialogTest {

    @Test
    public void testExtractGistId_fromFullUrl() {
        String id = AddGistLinkDialog.extractGistId(
                "https://gist.github.com/octocat/6cad326836d38bd3a7ae");

        assertThat(id).isEqualTo("6cad326836d38bd3a7ae");
    }

    @Test
    public void testExtractGistId_fromUrlWithTrailingSlash() {
        String id = AddGistLinkDialog.extractGistId(
                "https://gist.github.com/octocat/6cad326836d38bd3a7ae/");

        assertThat(id).isEqualTo("6cad326836d38bd3a7ae");
    }

    @Test
    public void testExtractGistId_fromBareId() {
        String id = AddGistLinkDialog.extractGistId("6cad326836d38bd3a7ae");

        assertThat(id).isEqualTo("6cad326836d38bd3a7ae");
    }

    @Test
    public void testExtractGistId_fromIdWithWhitespace() {
        String id = AddGistLinkDialog.extractGistId("  6cad326836d38bd3a7ae  ");

        assertThat(id).isEqualTo("6cad326836d38bd3a7ae");
    }

    @Test
    public void testExtractGistId_invalidInput_returnsNull() {
        assertThat(AddGistLinkDialog.extractGistId("not-a-gist")).isNull();
    }

    @Test
    public void testExtractGistId_emptyInput_returnsNull() {
        assertThat(AddGistLinkDialog.extractGistId("")).isNull();
    }

    @Test
    public void testExtractGistId_nullInput_returnsNull() {
        assertThat(AddGistLinkDialog.extractGistId(null)).isNull();
    }

    @Test
    public void testExtractGistId_wrongDomain_returnsNull() {
        assertThat(AddGistLinkDialog.extractGistId(
                "https://github.com/octocat/6cad326836d38bd3a7ae")).isNull();
    }
}
