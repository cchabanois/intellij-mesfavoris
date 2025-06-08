package mesfavoris.internal.settings;


import mesfavoris.placeholders.PathPlaceholder;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Test for PlaceholdersTableModel with modal editing
 */
public class PlaceholdersTableModelModalTest {

    @Test
    public void testTableIsNotEditable() {
        // Given
        PlaceholdersTableModel model = new PlaceholdersTableModel();
        PathPlaceholder placeholder = new PathPlaceholder("HOME", Paths.get("/home/user"));
        model.addPlaceholder(placeholder);

        // When & Then
        assertFalse("Name column should not be editable", model.isCellEditable(0, 0));
        assertFalse("Path column should not be editable", model.isCellEditable(0, 1));
    }

    @Test
    public void testAddPlaceholderWithPathPlaceholder() {
        // Given
        PlaceholdersTableModel model = new PlaceholdersTableModel();
        PathPlaceholder placeholder = new PathPlaceholder("HOME", Paths.get("/home/user"));

        // When
        model.addPlaceholder(placeholder);

        // Then
        assertEquals("Should have 1 placeholder", 1, model.getRowCount());
        assertEquals("Name should match", "HOME", model.getValueAt(0, 0));
        assertEquals("Path should match", "/home/user", model.getValueAt(0, 1));
    }

    @Test
    public void testUpdatePlaceholder() {
        // Given
        PlaceholdersTableModel model = new PlaceholdersTableModel();
        PathPlaceholder original = new PathPlaceholder("HOME", Paths.get("/home/user"));
        model.addPlaceholder(original);

        PathPlaceholder updated = new PathPlaceholder("WORK", Paths.get("/work/projects"));

        // When
        model.updatePlaceholder(0, updated);

        // Then
        assertEquals("Name should be updated", "WORK", model.getValueAt(0, 0));
        assertEquals("Path should be updated", "/work/projects", model.getValueAt(0, 1));
    }

    @Test
    public void testGetPlaceholder() {
        // Given
        PlaceholdersTableModel model = new PlaceholdersTableModel();
        PathPlaceholder placeholder = new PathPlaceholder("HOME", Paths.get("/home/user"));
        model.addPlaceholder(placeholder);

        // When
        PathPlaceholder retrieved = model.getPlaceholder(0);

        // Then
        assertNotNull("Should return placeholder", retrieved);
        assertEquals("Name should match", "HOME", retrieved.getName());
        assertEquals("Path should match", Paths.get("/home/user").toAbsolutePath(), retrieved.getPath());
    }

    @Test
    public void testPlaceholderNamesAreUppercase() {
        // Given
        PlaceholdersTableModel model = new PlaceholdersTableModel();
        PathPlaceholder placeholder = new PathPlaceholder("home", Paths.get("/home/user"));

        // When
        model.addPlaceholder(placeholder);

        // Then
        assertEquals("Name should be uppercase", "HOME", model.getValueAt(0, 0));
    }

    @Test
    public void testMultiplePlaceholdersWithDifferentNames() {
        // Given
        PlaceholdersTableModel model = new PlaceholdersTableModel();
        PathPlaceholder home = new PathPlaceholder("HOME", Paths.get("/home/user"));
        PathPlaceholder work = new PathPlaceholder("WORK", Paths.get("/work/projects"));
        PathPlaceholder temp = new PathPlaceholder("TEMP", Paths.get("/tmp"));

        // When
        model.addPlaceholder(home);
        model.addPlaceholder(work);
        model.addPlaceholder(temp);

        // Then
        assertEquals("Should have 3 placeholders", 3, model.getRowCount());
        assertEquals("First placeholder name", "HOME", model.getValueAt(0, 0));
        assertEquals("Second placeholder name", "WORK", model.getValueAt(1, 0));
        assertEquals("Third placeholder name", "TEMP", model.getValueAt(2, 0));
    }
}
