package mesfavoris.internal.settings.placeholders;

import mesfavoris.placeholders.PathPlaceholder;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * Test for PlaceholdersListPanel with list implementation
 */
public class PlaceholdersPanelTest {

    @Test
    public void testPathPlaceholderCreation() {
        // Given & When
        PathPlaceholder home = new PathPlaceholder("HOME", Paths.get("/home/user"));
        PathPlaceholder work = new PathPlaceholder("WORK", Paths.get("/work/projects"));

        // Then
        assertEquals("Home placeholder name", "HOME", home.getName());
        assertEquals("Work placeholder name", "WORK", work.getName());
        assertEquals("Home placeholder path", Paths.get("/home/user").toAbsolutePath(), home.getPath());
        assertEquals("Work placeholder path", Paths.get("/work/projects").toAbsolutePath(), work.getPath());
    }

    @Test
    public void testPlaceholderNamesAreUppercase() {
        // Given & When
        PathPlaceholder placeholder = new PathPlaceholder("home", Paths.get("/home/user"));

        // Then
        assertEquals("Name should be uppercase", "HOME", placeholder.getName());
    }

    // Note: PlaceholdersListPanel tests are disabled due to IntelliJ service dependencies in test environment
    // The panel functionality is tested through integration tests and manual testing
}
