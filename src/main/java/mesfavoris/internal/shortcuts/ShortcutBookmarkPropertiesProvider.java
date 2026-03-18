package mesfavoris.internal.shortcuts;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.bookmarktype.AbstractBookmarkPropertiesProvider;
import mesfavoris.model.Bookmark;

import java.util.Map;

public class ShortcutBookmarkPropertiesProvider extends AbstractBookmarkPropertiesProvider {

	@Override
	public void addBookmarkProperties(Map<String, String> bookmarkProperties, DataContext dataContext, ProgressIndicator progress) {
		Object selected = dataContext.getData(PlatformDataKeys.SELECTED_ITEM);
		if (!(selected instanceof Bookmark bookmark)) {
			return;
		}
        putIfAbsent(bookmarkProperties, Bookmark.PROPERTY_NAME,
				bookmark.getPropertyValue(Bookmark.PROPERTY_NAME) + " shortcut");
		putIfAbsent(bookmarkProperties, ShortcutBookmarkProperties.PROP_BOOKMARK_ID, bookmark.getId().toString());
	}

}
