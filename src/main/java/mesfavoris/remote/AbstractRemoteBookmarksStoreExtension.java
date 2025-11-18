package mesfavoris.remote;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Abstract base class for remote bookmarks store extensions.
 * Provides a convenient way to implement RemoteBookmarksStoreExtension.
 */
public abstract class AbstractRemoteBookmarksStoreExtension implements RemoteBookmarksStoreExtension {

    private final String id;
    private final String label;
    private final Icon icon;
    private final Icon overlayIcon;

    /**
     * Constructor for remote bookmarks store extension
     *
     * @param id The unique identifier for this remote bookmarks store
     * @param label The display label for this remote bookmarks store
     * @param icon The icon for this remote bookmarks store
     * @param overlayIcon The overlay icon for this remote bookmarks store
     */
    protected AbstractRemoteBookmarksStoreExtension(
            @NotNull String id,
            @NotNull String label,
            @NotNull Icon icon,
            @NotNull Icon overlayIcon) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.overlayIcon = overlayIcon;
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public String getLabel() {
        return label;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return icon;
    }

    @NotNull
    @Override
    public Icon getOverlayIcon() {
        return overlayIcon;
    }

    /**
     * Create a new instance of the remote bookmarks store for the given project.
     * Subclasses must implement this method to return their specific store implementation.
     *
     * @param project The project for which to create the store
     * @return A new instance of the remote bookmarks store
     */
    @NotNull
    @Override
    public abstract AbstractRemoteBookmarksStore createStore(@NotNull Project project);
}

