package mesfavoris.internal.settings.bookmarktypes;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import mesfavoris.bookmarktype.IDisabledBookmarkTypesProvider;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Persistent store for bookmark types settings
 */
@State(
    name = "BookmarkTypesStore",
    storages = @Storage("bookmark-types.xml")
)
public class BookmarkTypesStore implements PersistentStateComponent<Element>, IDisabledBookmarkTypesProvider {
    
    private final Set<String> disabledBookmarkTypes = new HashSet<>();

    public static BookmarkTypesStore getInstance() {
        return ApplicationManager.getApplication().getService(BookmarkTypesStore.class);
    }

    @Override
    public @Nullable Element getState() {
        Element container = new Element("BookmarkTypesStore");
        for (String disabledType : disabledBookmarkTypes) {
            Element typeElement = new Element("disabledType");
            typeElement.setAttribute("name", disabledType);
            container.addContent(typeElement);
        }
        return container;
    }

    @Override
    public void loadState(@NotNull Element state) {
        disabledBookmarkTypes.clear();
        for (Element typeElement : state.getChildren("disabledType")) {
            String name = typeElement.getAttributeValue("name");
            if (name != null && !name.trim().isEmpty()) {
                disabledBookmarkTypes.add(name.trim());
            }
        }
    }

    /**
     * Check if a bookmark type is enabled
     * @param bookmarkTypeName the name of the bookmark type
     * @return true if enabled, false if disabled
     */
    @Override
    public boolean isBookmarkTypeEnabled(String bookmarkTypeName) {
        return !disabledBookmarkTypes.contains(bookmarkTypeName);
    }



    /**
     * Get all disabled bookmark type names
     * @return set of disabled bookmark type names
     */
    @Override
    public Set<String> getDisabledBookmarkTypes() {
        return Collections.unmodifiableSet(disabledBookmarkTypes);
    }

    /**
     * Set all disabled bookmark types
     * @param disabledTypes set of bookmark type names to disable
     */
    public void setDisabledBookmarkTypes(Set<String> disabledTypes) {
        disabledBookmarkTypes.clear();
        disabledBookmarkTypes.addAll(disabledTypes);
    }
}
