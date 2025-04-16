package mesfavoris.bookmarktype;

import java.util.Map;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.progress.ProgressIndicator;

public interface IBookmarkPropertiesProvider {

	/**
	 * Get bookmark properties, depending on the current
	 * and current selection
	 * 
	 * @param bookmarkProperties
	 * @param dataContext
	 */
	public abstract void addBookmarkProperties(Map<String, String> bookmarkProperties, DataContext dataContext, ProgressIndicator progress);

}