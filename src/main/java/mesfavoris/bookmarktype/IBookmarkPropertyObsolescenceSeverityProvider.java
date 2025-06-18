package mesfavoris.bookmarktype;

import mesfavoris.model.Bookmark;

import java.util.Map;

/**
 * Provides the severity in case a property changed
 * 
 * @author cchabanois
 *
 */
public interface IBookmarkPropertyObsolescenceSeverityProvider {

	enum ObsolescenceSeverity {
		// ignore the new value
		IGNORE,
		// only info, property does not really need to be updated
		INFO,
		// warning, property should be updated to the new value
		WARNING
	}

	/**
	 * Get the obsolescence severity for a given property
	 * @param bookmark
	 * @param obsoleteProperties
	 *            key is the name of the obsolete property, value is the new
	 *            value
	 * @param propertyName
	 *            the property name
	 * @return the severity
	 */
	ObsolescenceSeverity getObsolescenceSeverity(Bookmark bookmark, Map<String, String> obsoleteProperties,
			String propertyName);

}
