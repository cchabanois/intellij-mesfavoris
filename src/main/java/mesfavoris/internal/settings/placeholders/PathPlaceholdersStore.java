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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Persistent store for path placeholders
 */
@State(
    name = "PathPlaceholdersStore",
    storages = @Storage("mesfavoris.xml")
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

    @Override
    public void initializeComponent() {
        if (get(PLACEHOLDER_HOME_NAME) == null) {
            Path userHome = getUserHome();
            if (userHome != null) {
                this.placeholders.add(0, new PathPlaceholder(PLACEHOLDER_HOME_NAME, userHome));
            }
        }
    }

    public List<PathPlaceholder> getPlaceholders() {
        // Ensure HOME placeholder is always first in the returned list
        List<PathPlaceholder> result = new ArrayList<>();
        PathPlaceholder homePlaceholder = null;

        for (PathPlaceholder placeholder : placeholders) {
            if (PLACEHOLDER_HOME_NAME.equals(placeholder.getName())) {
                homePlaceholder = placeholder;
            } else {
                result.add(placeholder);
            }
        }

        // Insert HOME placeholder at the beginning if it exists
        if (homePlaceholder != null) {
            result.add(0, homePlaceholder);
        }

        return result;
    }

    public void setPlaceholders(List<PathPlaceholder> placeholders) {
        this.placeholders.clear();

        // Ensure HOME placeholder is always first
        PathPlaceholder homePlaceholder = null;
        List<PathPlaceholder> otherPlaceholders = new ArrayList<>();

        for (PathPlaceholder placeholder : placeholders) {
            if (PLACEHOLDER_HOME_NAME.equals(placeholder.getName())) {
                homePlaceholder = placeholder;
            } else {
                otherPlaceholders.add(placeholder);
            }
        }

        // Add HOME placeholder first if it exists
        if (homePlaceholder != null) {
            this.placeholders.add(homePlaceholder);
        }

        // Add all other placeholders
        this.placeholders.addAll(otherPlaceholders);
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

    private Path getUserHome() {
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.trim().isEmpty()) {
            try {
                return Paths.get(userHome);
            } catch (Exception e) {
                // Return null if path is invalid
                return null;
            }
        }
        return null;
    }
}
