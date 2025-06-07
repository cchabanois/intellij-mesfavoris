package mesfavoris.internal.settings;


import mesfavoris.placeholders.IPathPlaceholders;
import mesfavoris.placeholders.PathPlaceholder;
import org.jdom.Element;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test for PathPlaceholdersStore
 */
public class PathPlaceholdersStoreTest {

    @Test
    public void testCreatePathPlaceholder() {
        // Given & When
        PathPlaceholder placeholder = new PathPlaceholder("HOME", Paths.get("/home/user"));

        // Then
        assertEquals("HOME", placeholder.getName());
        assertEquals(Paths.get("/home/user").toAbsolutePath(), placeholder.getPath());
    }

    @Test
    public void testGetPlaceholders() {
        // Given
        PathPlaceholdersStore store = new PathPlaceholdersStore();
        List<PathPlaceholder> placeholderList = Arrays.asList(
            new PathPlaceholder("HOME", Paths.get("/home/user")),
            new PathPlaceholder("WORK", Paths.get("/work/projects"))
        );
        store.setPlaceholders(placeholderList);

        // When
        List<PathPlaceholder> retrievedPlaceholders = store.getPlaceholders();

        // Then
        assertEquals(2, retrievedPlaceholders.size());
        assertEquals("HOME", retrievedPlaceholders.get(0).getName());
        assertEquals("WORK", retrievedPlaceholders.get(1).getName());
    }

    @Test
    public void testIPathPlaceholdersInterface() {
        // Given
        PathPlaceholdersStore store = new PathPlaceholdersStore();
        List<PathPlaceholder> placeholderList = Arrays.asList(
            new PathPlaceholder("HOME", Paths.get("/home/user")),
            new PathPlaceholder("WORK", Paths.get("/work/projects"))
        );
        store.setPlaceholders(placeholderList);

        // When - use as IPathPlaceholders
        IPathPlaceholders placeholders = store;

        // Then - test get method
        PathPlaceholder homePlaceholder = placeholders.get("HOME");
        assertNotNull("HOME placeholder should exist", homePlaceholder);
        assertEquals("HOME", homePlaceholder.getName());
        assertEquals(Paths.get("/home/user").toAbsolutePath(), homePlaceholder.getPath());

        PathPlaceholder workPlaceholder = placeholders.get("WORK");
        assertNotNull("WORK placeholder should exist", workPlaceholder);
        assertEquals("WORK", workPlaceholder.getName());

        PathPlaceholder nonExistent = placeholders.get("NONEXISTENT");
        assertNull("Non-existent placeholder should return null", nonExistent);

        // Test iterator
        Iterator<PathPlaceholder> iterator = placeholders.iterator();
        assertTrue("Iterator should have elements", iterator.hasNext());

        int count = 0;
        for (PathPlaceholder placeholder : placeholders) {
            assertNotNull("Placeholder should not be null", placeholder);
            count++;
        }
        assertEquals("Should iterate over 2 placeholders", 2, count);
    }

    @Test
    public void testXmlSerialization() {
        // Given
        PathPlaceholdersStore store = new PathPlaceholdersStore();
        List<PathPlaceholder> originalData = Arrays.asList(
            new PathPlaceholder("HOME", Paths.get("/home/user")),
            new PathPlaceholder("WORK", Paths.get("/work/projects")),
            new PathPlaceholder("TEMP", Paths.get("/tmp"))
        );
        store.setPlaceholders(originalData);

        // When - serialize to XML
        Element xmlState = store.getState();

        // Then - verify XML structure
        assertNotNull("XML state should not be null", xmlState);
        assertEquals("Root element name", "PathPlaceholdersStore", xmlState.getName());
        assertEquals("Should have 3 placeholder elements", 3, xmlState.getChildren("placeholder").size());

        // When - deserialize from XML
        PathPlaceholdersStore newStore = new PathPlaceholdersStore();
        newStore.loadState(xmlState);

        // Then - verify data is correctly restored
        List<PathPlaceholder> restoredData = newStore.getPlaceholders();
        assertEquals("Should have 3 placeholders", 3, restoredData.size());

        // Verify each placeholder
        assertEquals("HOME", restoredData.get(0).getName());
        assertEquals(Paths.get("/home/user").toAbsolutePath(), restoredData.get(0).getPath());
        assertEquals("WORK", restoredData.get(1).getName());
        assertEquals(Paths.get("/work/projects").toAbsolutePath(), restoredData.get(1).getPath());
        assertEquals("TEMP", restoredData.get(2).getName());
        assertEquals(Paths.get("/tmp").toAbsolutePath(), restoredData.get(2).getPath());
    }

    @Test
    public void testXmlSerializationWithEmptyData() {
        // Given
        PathPlaceholdersStore store = new PathPlaceholdersStore();

        // When - serialize empty store
        Element xmlState = store.getState();

        // Then
        assertNotNull("XML state should not be null", xmlState);
        assertEquals("Root element name", "PathPlaceholdersStore", xmlState.getName());
        assertEquals("Should have no placeholder elements", 0, xmlState.getChildren("placeholder").size());

        // When - deserialize
        PathPlaceholdersStore newStore = new PathPlaceholdersStore();
        newStore.loadState(xmlState);

        // Then
        List<PathPlaceholder> restoredData = newStore.getPlaceholders();
        assertEquals("Should have no placeholders", 0, restoredData.size());
    }

    @Test
    public void testSetPlaceholdersDoesNotModifyInputList() {
        // Given
        PathPlaceholdersStore store = new PathPlaceholdersStore();
        List<PathPlaceholder> inputList = Arrays.asList(
            new PathPlaceholder("HOME", Paths.get("/home/user")),
            new PathPlaceholder("WORK", Paths.get("/work/projects"))
        );
        int originalSize = inputList.size();

        // When - set placeholders
        store.setPlaceholders(inputList);

        // Then - input list should remain unchanged
        assertEquals("Input list size should remain unchanged", originalSize, inputList.size());
        assertEquals("Input list should still contain HOME", "HOME", inputList.get(0).getName());
        assertEquals("Input list should still contain WORK", "WORK", inputList.get(1).getName());

        // And - store should contain the data
        List<PathPlaceholder> storedPlaceholders = store.getPlaceholders();
        assertEquals("Store should contain 2 placeholders", 2, storedPlaceholders.size());
        assertEquals("Store should contain HOME", "HOME", storedPlaceholders.get(0).getName());
        assertEquals("Store should contain WORK", "WORK", storedPlaceholders.get(1).getName());
    }
}
