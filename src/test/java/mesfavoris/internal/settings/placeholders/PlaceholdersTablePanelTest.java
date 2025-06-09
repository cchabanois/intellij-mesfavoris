package mesfavoris.internal.settings.placeholders;

import mesfavoris.placeholders.PathPlaceholder;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test for PlaceholdersTablePanel with table implementation
 */
public class PlaceholdersTablePanelTest {

    @Test
    public void testSetAndGetPlaceholders() {
        // Given
        PlaceholdersTablePanel panel = new PlaceholdersTablePanel();
        PathPlaceholder home = new PathPlaceholder("HOME", Paths.get("/home/user"));
        PathPlaceholder work = new PathPlaceholder("WORK", Paths.get("/work/projects"));
        List<PathPlaceholder> placeholders = Arrays.asList(home, work);

        // When
        panel.setPlaceholders(placeholders);
        List<PathPlaceholder> result = panel.getPlaceholders();

        // Then
        assertEquals("Should have 2 placeholders", 2, result.size());
        assertEquals("First placeholder name", "HOME", result.get(0).getName());
        assertEquals("Second placeholder name", "WORK", result.get(1).getName());
        assertEquals("First placeholder path", Paths.get("/home/user").toAbsolutePath(), result.get(0).getPath());
        assertEquals("Second placeholder path", Paths.get("/work/projects").toAbsolutePath(), result.get(1).getPath());
    }

    @Test
    public void testEmptyPlaceholdersList() {
        // Given
        PlaceholdersTablePanel panel = new PlaceholdersTablePanel();

        // When
        List<PathPlaceholder> result = panel.getPlaceholders();

        // Then
        assertTrue("Should return empty list", result.isEmpty());
    }

    @Test
    public void testIsModified() {
        // Given
        PlaceholdersTablePanel panel = new PlaceholdersTablePanel();
        PathPlaceholder home = new PathPlaceholder("HOME", Paths.get("/home/user"));
        List<PathPlaceholder> original = List.of(home);
        panel.setPlaceholders(original);

        // When - no changes
        boolean isModified1 = panel.isModified(original);

        // When - add placeholder
        PathPlaceholder work = new PathPlaceholder("WORK", Paths.get("/work"));
        panel.setPlaceholders(Arrays.asList(home, work));
        boolean isModified2 = panel.isModified(original);

        // Then
        assertFalse("Should not be modified when no changes", isModified1);
        assertTrue("Should be modified when placeholders added", isModified2);
    }
}
