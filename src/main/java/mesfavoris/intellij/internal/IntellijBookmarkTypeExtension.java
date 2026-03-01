package mesfavoris.intellij.internal;

import com.intellij.icons.AllIcons;
import mesfavoris.extensions.AbstractBookmarkTypeExtension;

public class IntellijBookmarkTypeExtension extends AbstractBookmarkTypeExtension  {

    public static final String BOOKMARK_TYPE_NAME = "Intellij";

    public IntellijBookmarkTypeExtension() {
        super(BOOKMARK_TYPE_NAME, "Intellij specific bookmarks", AllIcons.General.Information);

        addPropertiesProvider(new ActionBookmarkPropertiesProvider());
        addLocationProvider(new ActionBookmarkLocationProvider());
        addLabelProvider(new ActionBookmarkLabelProvider());
        addGotoBookmarkHandler(new GotoActionBookmark());
    }
}
