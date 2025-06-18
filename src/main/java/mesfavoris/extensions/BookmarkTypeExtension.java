package mesfavoris.extensions;

import com.intellij.openapi.extensions.ExtensionPointName;
import mesfavoris.bookmarktype.*;
import mesfavoris.ui.details.IBookmarkDetailPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * Extension point for defining bookmark types
 *
 */
public interface BookmarkTypeExtension {
    ExtensionPointName<BookmarkTypeExtension> EP_NAME = 
        ExtensionPointName.create("com.cchabanois.mesfavoris.bookmarkType");

    /**
     * @return The unique name of this bookmark type
     */
    @NotNull
    String getName();

    /**
     * @return The icon for this bookmark type, or null for default
     */
    @Nullable
    Icon getIcon();

    /**
     * @return The property descriptors defined by this bookmark type
     */
    @NotNull
    List<BookmarkPropertyDescriptor> getPropertyDescriptors();

    /**
     * Get a specific property descriptor by name
     * @param propertyName The name of the property to find
     * @return The property descriptor, or null if not found
     */
    @Nullable
    BookmarkPropertyDescriptor getPropertyDescriptor(@NotNull String propertyName);

    /**
     * @return The properties providers for this bookmark type with their priorities
     */
    @NotNull
    List<PrioritizedElement<IBookmarkPropertiesProvider>> getPropertiesProviders();

    /**
     * @return The label providers for this bookmark type with their priorities
     */
    @NotNull
    List<PrioritizedElement<IBookmarkLabelProvider>> getLabelProviders();

    /**
     * @return The location providers for this bookmark type with their priorities
     */
    @NotNull
    List<PrioritizedElement<IBookmarkLocationProvider>> getLocationProviders();

    /**
     * @return The goto bookmark handlers for this bookmark type with their priorities
     */
    @NotNull
    List<PrioritizedElement<IGotoBookmark>> getGotoBookmarkHandlers();

    /**
     * @return The marker attributes providers for this bookmark type with their priorities
     */
    @NotNull
    List<PrioritizedElement<IBookmarkMarkerAttributesProvider>> getMarkerAttributesProviders();

    /**
     * @return The detail parts for this bookmark type with their priorities
     */
    @NotNull
    List<PrioritizedElement<IBookmarkDetailPart>> getDetailParts();
}
