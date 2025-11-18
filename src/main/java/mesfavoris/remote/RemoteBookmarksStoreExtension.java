package mesfavoris.remote;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Extension point for defining remote bookmarks storage providers.
 * This allows plugins to contribute their own remote storage implementations
 * (e.g., Google Drive, Dropbox, etc.)
 */
public interface RemoteBookmarksStoreExtension {
    ExtensionPointName<RemoteBookmarksStoreExtension> EP_NAME =
            ExtensionPointName.create("com.cchabanois.mesfavoris.remoteBookmarksStore");

    /**
     * @return The unique identifier for this remote bookmarks store
     */
    @NotNull
    String getId();

    /**
     * @return The display label for this remote bookmarks store
     */
    @NotNull
    String getLabel();

    /**
     * @return The icon for this remote bookmarks store
     */
    @NotNull
    Icon getIcon();

    /**
     * @return The overlay icon for this remote bookmarks store (displayed on bookmarks)
     */
    @NotNull
    Icon getOverlayIcon();

    /**
     * Create a new instance of the remote bookmarks store for the given project.
     * The returned instance should extend AbstractRemoteBookmarksStore.
     *
     * @param project The project for which to create the store
     * @return A new instance of the remote bookmarks store
     */
    @NotNull
    AbstractRemoteBookmarksStore createStore(@NotNull Project project);
}

