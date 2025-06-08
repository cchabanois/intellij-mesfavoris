package mesfavoris.internal.settings.placeholders;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import mesfavoris.placeholders.IPathPlaceholders;
import mesfavoris.placeholders.PathPlaceholder;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Persistent store for path placeholders
 */
@State(
    name = "PathPlaceholdersStore",
    storages = @Storage("path-placeholders.xml")
)
public class PathPlaceholdersStore implements PersistentStateComponent<Element>, IPathPlaceholders {

    private final List<PathPlaceholder> placeholders = new ArrayList<>();

    public static PathPlaceholdersStore getInstance() {
        return ApplicationManager.getApplication().getService(PathPlaceholdersStore.class);
    }

    @Override
    public @Nullable Element getState() {
        Element container = new Element("PathPlaceholdersStore");
        for (PathPlaceholder placeholder : placeholders) {
            Element placeholderElement = new Element("placeholder");
            placeholderElement.setAttribute("name", placeholder.getName());
            placeholderElement.setAttribute("path", placeholder.getPath().toString());
            container.addContent(placeholderElement);
        }
        return container;
    }

    @Override
    public void loadState(@NotNull Element state) {
        placeholders.clear();
        for (Element placeholderElement : state.getChildren("placeholder")) {
            String name = placeholderElement.getAttributeValue("name");
            String path = placeholderElement.getAttributeValue("path");
            if (name != null && !name.trim().isEmpty() && path != null && !path.trim().isEmpty()) {
                try {
                    placeholders.add(new PathPlaceholder(name.trim(), Paths.get(path.trim())));
                } catch (Exception e) {
                    // Skip invalid placeholders
                }
            }
        }
    }

    public List<PathPlaceholder> getPlaceholders() {
        return new ArrayList<>(placeholders);
    }

    public void setPlaceholders(List<PathPlaceholder> placeholders) {
        this.placeholders.clear();
        this.placeholders.addAll(placeholders);
    }

    @Override
    public @NotNull Iterator<PathPlaceholder> iterator() {
        return placeholders.iterator();
    }

    @Override
    public PathPlaceholder get(String name) {
        if (name == null) {
            return null;
        }
        for (PathPlaceholder placeholder : placeholders) {
            if (name.equals(placeholder.getName())) {
                return placeholder;
            }
        }
        return null;
    }
}
