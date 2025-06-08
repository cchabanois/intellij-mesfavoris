package mesfavoris.internal.settings;

import mesfavoris.internal.placeholders.PathPlaceholderResolver;
import mesfavoris.internal.settings.placeholders.PathPlaceholdersStore;
import mesfavoris.placeholders.IPathPlaceholderResolver;
import mesfavoris.placeholders.PathPlaceholder;
import org.jdom.Element;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration test for XML serialization with placeholder resolution
 */
public class PathPlaceholdersXmlIntegrationTest {

    @Test
    public void testXmlSerializationWithPlaceholderResolution() {
        // Given - create store with placeholders
        PathPlaceholdersStore store = new PathPlaceholdersStore();
        List<PathPlaceholder> placeholders = Arrays.asList(
            new PathPlaceholder("HOME", Paths.get("/home/user")),
            new PathPlaceholder("WORK", Paths.get("/work/projects")),
            new PathPlaceholder("TEMP", Paths.get("/tmp"))
        );
        store.setPlaceholders(placeholders);

        // When - serialize to XML
        Element xmlState = store.getState();

        // Create new store and deserialize
        PathPlaceholdersStore newStore = new PathPlaceholdersStore();
        newStore.loadState(xmlState);

        // Then - verify placeholder resolution works
        IPathPlaceholderResolver resolver = new PathPlaceholderResolver(newStore);
        
        // Test expansion
        Path expandedPath = resolver.expand("${HOME}/documents");
        assertNotNull("Expanded path should not be null", expandedPath);
        assertEquals("Should expand HOME placeholder", 
            Paths.get("/home/user/documents").toAbsolutePath(), expandedPath);

        Path workPath = resolver.expand("${WORK}/myproject");
        assertNotNull("Work path should not be null", workPath);
        assertEquals("Should expand WORK placeholder",
            Paths.get("/work/projects/myproject").toAbsolutePath(), workPath);

        // Test collapse
        String collapsedPath = resolver.collapse(Paths.get("/home/user/documents/file.txt"));
        assertEquals("Should collapse to HOME placeholder", "${HOME}/documents/file.txt", collapsedPath);
    }

    @Test
    public void testXmlSerializationPreservesOrder() {
        // Given
        PathPlaceholdersStore store = new PathPlaceholdersStore();
        List<PathPlaceholder> originalOrder = Arrays.asList(
            new PathPlaceholder("ALPHA", Paths.get("/alpha")),
            new PathPlaceholder("BETA", Paths.get("/beta")),
            new PathPlaceholder("GAMMA", Paths.get("/gamma"))
        );
        store.setPlaceholders(originalOrder);

        // When - serialize and deserialize
        Element xmlState = store.getState();
        PathPlaceholdersStore newStore = new PathPlaceholdersStore();
        newStore.loadState(xmlState);

        // Then - verify order is preserved
        List<PathPlaceholder> restoredOrder = newStore.getPlaceholders();
        assertEquals("Should have same number of placeholders", 3, restoredOrder.size());
        assertEquals("First placeholder name", "ALPHA", restoredOrder.get(0).getName());
        assertEquals("Second placeholder name", "BETA", restoredOrder.get(1).getName());
        assertEquals("Third placeholder name", "GAMMA", restoredOrder.get(2).getName());
    }

    @Test
    public void testXmlSerializationWithSpecialCharacters() {
        // Given - placeholders with special characters
        PathPlaceholdersStore store = new PathPlaceholdersStore();
        List<PathPlaceholder> placeholders = Arrays.asList(
            new PathPlaceholder("PATH_WITH_SPACES", Paths.get("/path with spaces")),
            new PathPlaceholder("PATH_WITH_UNICODE", Paths.get("/path/with/üñíçødé")),
            new PathPlaceholder("PATH_WITH_QUOTES", Paths.get("/path/with/\"quotes\""))
        );
        store.setPlaceholders(placeholders);

        // When - serialize and deserialize
        Element xmlState = store.getState();
        PathPlaceholdersStore newStore = new PathPlaceholdersStore();
        newStore.loadState(xmlState);

        // Then - verify special characters are preserved
        List<PathPlaceholder> restored = newStore.getPlaceholders();
        assertEquals("Should preserve spaces", Paths.get("/path with spaces").toAbsolutePath(), restored.get(0).getPath());
        assertEquals("Should preserve unicode", Paths.get("/path/with/üñíçødé").toAbsolutePath(), restored.get(1).getPath());
        assertEquals("Should preserve quotes", Paths.get("/path/with/\"quotes\"").toAbsolutePath(), restored.get(2).getPath());
    }

    @Test
    public void testXmlSerializationWithValidPlaceholders() {
        // Given - only valid placeholders (PathPlaceholder constructor validates)
        PathPlaceholdersStore store = new PathPlaceholdersStore();
        List<PathPlaceholder> placeholders = List.of(
                new PathPlaceholder("VALID", Paths.get("/valid/path"))
        );
        store.setPlaceholders(placeholders);

        // When - serialize and deserialize
        Element xmlState = store.getState();
        PathPlaceholdersStore newStore = new PathPlaceholdersStore();
        newStore.loadState(xmlState);

        // Then - verify placeholder is preserved
        List<PathPlaceholder> restored = newStore.getPlaceholders();
        assertEquals("Should have 1 valid placeholder", 1, restored.size());
        assertEquals("Valid placeholder name", "VALID", restored.get(0).getName());
        assertEquals("Valid placeholder path", Paths.get("/valid/path").toAbsolutePath(), restored.get(0).getPath());
    }
}
