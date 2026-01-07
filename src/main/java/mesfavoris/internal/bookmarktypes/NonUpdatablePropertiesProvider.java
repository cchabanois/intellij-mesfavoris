package mesfavoris.internal.bookmarktypes;

import mesfavoris.bookmarktype.IBookmarkPropertyDescriptors;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Returns the list of bookmark properties that are non updateable
 */
public class NonUpdatablePropertiesProvider implements Supplier<Set<String>> {
	private final IBookmarkPropertyDescriptors bookmarkPropertyDescriptors;

	public NonUpdatablePropertiesProvider(IBookmarkPropertyDescriptors bookmarkPropertyDescriptors) {
		this.bookmarkPropertyDescriptors = bookmarkPropertyDescriptors;
	}
	
	@Override
	public Set<String> get() {
		Set<String> nonUpdatableProperties = bookmarkPropertyDescriptors.getPropertyDescriptors().stream()
				.filter(descriptor -> !descriptor.isUpdatable()).map(descriptor -> descriptor.getName())
				.collect(Collectors.toSet());
		return nonUpdatableProperties;
	}
	
}
