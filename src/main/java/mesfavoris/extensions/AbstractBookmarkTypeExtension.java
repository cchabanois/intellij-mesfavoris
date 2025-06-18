package mesfavoris.extensions;

import mesfavoris.bookmarktype.*;
import mesfavoris.ui.details.IBookmarkDetailPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for bookmark type extensions.
 * Provides default implementations and builder-style methods for easier extension creation.
 */
public abstract class AbstractBookmarkTypeExtension implements BookmarkTypeExtension {
    
    private final String name;
    private final Icon icon;
    private final List<BookmarkPropertyDescriptor> properties = new ArrayList<>();
    private final List<PrioritizedElement<IBookmarkPropertiesProvider>> propertiesProviders = new ArrayList<>();
    private final List<PrioritizedElement<IBookmarkLabelProvider>> labelProviders = new ArrayList<>();
    private final List<PrioritizedElement<IBookmarkLocationProvider>> locationProviders = new ArrayList<>();
    private final List<PrioritizedElement<IGotoBookmark>> gotoBookmarkHandlers = new ArrayList<>();
    private final List<PrioritizedElement<IBookmarkMarkerAttributesProvider>> markerAttributesProviders = new ArrayList<>();
    private final List<PrioritizedElement<IBookmarkDetailPart>> detailParts = new ArrayList<>();
    
    protected AbstractBookmarkTypeExtension(@NotNull String name, @Nullable Icon icon) {
        this.name = name;
        this.icon = icon;
    }
    
    @NotNull
    @Override
    public String getName() {
        return name;
    }
    
    @Nullable
    @Override
    public Icon getIcon() {
        return icon;
    }
    
    @NotNull
    @Override
    public List<BookmarkPropertyDescriptor> getPropertyDescriptors() {
        return Collections.unmodifiableList(properties);
    }

    @Nullable
    @Override
    public BookmarkPropertyDescriptor getPropertyDescriptor(@NotNull String propertyName) {
        return properties.stream()
                .filter(property -> propertyName.equals(property.getName()))
                .findFirst()
                .orElse(null);
    }
    
    @NotNull
    @Override
    public List<PrioritizedElement<IBookmarkPropertiesProvider>> getPropertiesProviders() {
        return Collections.unmodifiableList(propertiesProviders);
    }

    @NotNull
    @Override
    public List<PrioritizedElement<IBookmarkLabelProvider>> getLabelProviders() {
        return Collections.unmodifiableList(labelProviders);
    }

    @NotNull
    @Override
    public List<PrioritizedElement<IBookmarkLocationProvider>> getLocationProviders() {
        return Collections.unmodifiableList(locationProviders);
    }

    @NotNull
    @Override
    public List<PrioritizedElement<IGotoBookmark>> getGotoBookmarkHandlers() {
        return Collections.unmodifiableList(gotoBookmarkHandlers);
    }

    @NotNull
    @Override
    public List<PrioritizedElement<IBookmarkMarkerAttributesProvider>> getMarkerAttributesProviders() {
        return Collections.unmodifiableList(markerAttributesProviders);
    }

    @NotNull
    @Override
    public List<PrioritizedElement<IBookmarkDetailPart>> getDetailParts() {
        return Collections.unmodifiableList(detailParts);
    }
    
    // Builder-style methods for configuration - protected for use in subclass constructors only

    protected AbstractBookmarkTypeExtension addProperty(@NotNull BookmarkPropertyDescriptor property) {
        this.properties.add(property);
        return this;
    }

    protected AbstractBookmarkTypeExtension addPropertiesProvider(@NotNull IBookmarkPropertiesProvider provider) {
        return addPropertiesProvider(provider, 10);
    }

    protected AbstractBookmarkTypeExtension addPropertiesProvider(@NotNull IBookmarkPropertiesProvider provider, int priority) {
        this.propertiesProviders.add(new PrioritizedElement<>(provider, priority));
        return this;
    }

    protected AbstractBookmarkTypeExtension addLabelProvider(@NotNull IBookmarkLabelProvider provider) {
        return addLabelProvider(provider, 10);
    }

    protected AbstractBookmarkTypeExtension addLabelProvider(@NotNull IBookmarkLabelProvider provider, int priority) {
        this.labelProviders.add(new PrioritizedElement<>(provider, priority));
        return this;
    }

    protected AbstractBookmarkTypeExtension addLocationProvider(@NotNull IBookmarkLocationProvider provider) {
        return addLocationProvider(provider, 10);
    }

    protected AbstractBookmarkTypeExtension addLocationProvider(@NotNull IBookmarkLocationProvider provider, int priority) {
        this.locationProviders.add(new PrioritizedElement<>(provider, priority));
        return this;
    }

    protected AbstractBookmarkTypeExtension addGotoBookmarkHandler(@NotNull IGotoBookmark handler) {
        return addGotoBookmarkHandler(handler, 10);
    }

    protected AbstractBookmarkTypeExtension addGotoBookmarkHandler(@NotNull IGotoBookmark handler, int priority) {
        this.gotoBookmarkHandlers.add(new PrioritizedElement<>(handler, priority));
        return this;
    }

    protected AbstractBookmarkTypeExtension addMarkerAttributesProvider(@NotNull IBookmarkMarkerAttributesProvider provider) {
        return addMarkerAttributesProvider(provider, 10);
    }

    protected AbstractBookmarkTypeExtension addMarkerAttributesProvider(@NotNull IBookmarkMarkerAttributesProvider provider, int priority) {
        this.markerAttributesProviders.add(new PrioritizedElement<>(provider, priority));
        return this;
    }

    protected AbstractBookmarkTypeExtension addDetailPart(@NotNull IBookmarkDetailPart detailPart) {
        return addDetailPart(detailPart, 10);
    }

    protected AbstractBookmarkTypeExtension addDetailPart(@NotNull IBookmarkDetailPart detailPart, int priority) {
        this.detailParts.add(new PrioritizedElement<>(detailPart, priority));
        return this;
    }
}
