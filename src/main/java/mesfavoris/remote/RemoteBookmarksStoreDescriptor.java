package mesfavoris.remote;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Descriptor for a remote bookmarks store containing its metadata.
 */
public record RemoteBookmarksStoreDescriptor(
        @NotNull String id,
        @NotNull String label,
        @NotNull Icon icon,
        @NotNull Icon iconOverlay
) {
}

