package mesfavoris.bookmarktype;

import mesfavoris.model.Bookmark;

import java.util.Map;

/**
 * Descriptor for a {@link Bookmark} property
 * 
 * @author cchabanois
 *
 */
public class BookmarkPropertyDescriptor implements IBookmarkPropertyObsolescenceSeverityProvider {
	private final String name;
	private final BookmarkPropertyType type;
	private final boolean updatable;
	private final String description;
	private final IBookmarkPropertyObsolescenceSeverityProvider bookmarkPropertyObsolescenceSeverityProvider;

	public enum BookmarkPropertyType {
		PATH, STRING, INT, INSTANT
	}

	public BookmarkPropertyDescriptor(String name, BookmarkPropertyType type, boolean updatable, String description,
			IBookmarkPropertyObsolescenceSeverityProvider bookmarkPropertyObsolescenceSeverityProvider) {
		this.name = name;
		this.type = type;
		this.updatable = updatable;
		this.description = description;
		this.bookmarkPropertyObsolescenceSeverityProvider = bookmarkPropertyObsolescenceSeverityProvider;
	}

	public String getDescription() {
		return description;
	}

	public String getName() {
		return name;
	}

	public BookmarkPropertyType getType() {
		return type;
	}

	@Override
	public ObsolescenceSeverity getObsolescenceSeverity(Bookmark bookmark, Map<String, String> obsoleteProperties, String propertyName) {
		return bookmarkPropertyObsolescenceSeverityProvider.getObsolescenceSeverity(bookmark, obsoleteProperties, propertyName);
	}

	public boolean isUpdatable() {
		return updatable;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + (updatable ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BookmarkPropertyDescriptor other = (BookmarkPropertyDescriptor) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type != other.type)
			return false;
		if (updatable != other.updatable)
			return false;
		return true;
	}

	/**
	 * Creates a new builder for BookmarkPropertyDescriptor
	 * @param name The property name (required)
	 * @return A new builder instance
	 */
	public static Builder bookmarkPropertyDescriptor(String name) {
		return new Builder(name);
	}

	/**
	 * Builder class for BookmarkPropertyDescriptor using the builder pattern
	 */
	public static class Builder {
		private final String name;
		private BookmarkPropertyType type = BookmarkPropertyType.STRING;
		private boolean updatable = true;
		private String description = null;
		private IBookmarkPropertyObsolescenceSeverityProvider obsolescenceSeverityProvider = null;

		private Builder(String name) {
			if (name == null || name.trim().isEmpty()) {
				throw new IllegalArgumentException("Property name cannot be null or empty");
			}
			this.name = name;
		}

		/**
		 * Sets the property type
		 * @param type The property type
		 * @return This builder instance
		 */
		public Builder type(BookmarkPropertyType type) {
			this.type = type;
			return this;
		}

		/**
		 * Sets whether the property is updatable
		 * @param updatable True if the property can be updated
		 * @return This builder instance
		 */
		public Builder updatable(boolean updatable) {
			this.updatable = updatable;
			return this;
		}

		/**
		 * Sets the property description
		 * @param description The property description
		 * @return This builder instance
		 */
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Sets the obsolescence severity provider
		 * @param provider The obsolescence severity provider
		 * @return This builder instance
		 */
		public Builder obsolescenceSeverityProvider(IBookmarkPropertyObsolescenceSeverityProvider provider) {
			this.obsolescenceSeverityProvider = provider;
			return this;
		}

		/**
		 * Builds the BookmarkPropertyDescriptor instance
		 * @return A new BookmarkPropertyDescriptor instance
		 */
		public BookmarkPropertyDescriptor build() {
			return new BookmarkPropertyDescriptor(name, type, updatable, description, obsolescenceSeverityProvider);
		}
	}

}
