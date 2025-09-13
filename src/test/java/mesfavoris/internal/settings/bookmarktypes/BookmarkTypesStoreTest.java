package mesfavoris.internal.settings.bookmarktypes;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.bookmarktype.IDisabledBookmarkTypesProvider;
import org.jdom.Element;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class BookmarkTypesStoreTest extends BasePlatformTestCase {

    @Test
    public void testBookmarkTypeEnabledByDefault() {
        // Given
        IDisabledBookmarkTypesProvider store = new BookmarkTypesStore();
        
        // When/Then
        assertThat(store.isBookmarkTypeEnabled("default")).isTrue();
        assertThat(store.isBookmarkTypeEnabled("java")).isTrue();
        assertThat(store.isBookmarkTypeEnabled("url")).isTrue();
    }
    
    @Test
    public void testSetDisabledBookmarkTypes() {
        // Given
        BookmarkTypesStore store = new BookmarkTypesStore();
        Set<String> disabledTypes = new HashSet<>();
        disabledTypes.add("java");
        disabledTypes.add("url");

        // When
        store.setDisabledBookmarkTypes(disabledTypes);

        // Then
        assertThat(store.isBookmarkTypeEnabled("default")).isTrue();
        assertThat(store.isBookmarkTypeEnabled("java")).isFalse();
        assertThat(store.isBookmarkTypeEnabled("url")).isFalse();
    }
    
    @Test
    public void testGetDisabledBookmarkTypes() {
        // Given
        BookmarkTypesStore store = new BookmarkTypesStore();
        Set<String> disabledTypes = new HashSet<>();
        disabledTypes.add("java");
        disabledTypes.add("url");
        store.setDisabledBookmarkTypes(disabledTypes);

        // When
        Set<String> retrievedDisabledTypes = store.getDisabledBookmarkTypes();

        // Then
        assertThat(retrievedDisabledTypes).containsExactlyInAnyOrder("java", "url");
    }
    
    @Test
    public void testPersistenceGetState() {
        // Given
        BookmarkTypesStore store = new BookmarkTypesStore();
        Set<String> disabledTypes = new HashSet<>();
        disabledTypes.add("java");
        disabledTypes.add("url");
        store.setDisabledBookmarkTypes(disabledTypes);

        // When
        Element state = store.getState();

        // Then
        assertThat(state).isNotNull();
        assertThat(state.getName()).isEqualTo("BookmarkTypesStore");
        assertThat(state.getChildren("disabledType")).hasSize(2);
    }
    
    @Test
    public void testPersistenceLoadState() {
        // Given
        BookmarkTypesStore store = new BookmarkTypesStore();
        Element state = new Element("BookmarkTypesStore");
        
        Element javaElement = new Element("disabledType");
        javaElement.setAttribute("name", "java");
        state.addContent(javaElement);
        
        Element urlElement = new Element("disabledType");
        urlElement.setAttribute("name", "url");
        state.addContent(urlElement);
        
        // When
        store.loadState(state);
        
        // Then
        assertThat(store.isBookmarkTypeEnabled("default")).isTrue();
        assertThat(store.isBookmarkTypeEnabled("java")).isFalse();
        assertThat(store.isBookmarkTypeEnabled("url")).isFalse();
    }
    
}
