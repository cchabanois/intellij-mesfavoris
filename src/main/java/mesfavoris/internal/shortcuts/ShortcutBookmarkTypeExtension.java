package mesfavoris.internal.shortcuts;

import com.intellij.icons.AllIcons;
import mesfavoris.extensions.AbstractBookmarkTypeExtension;

public class ShortcutBookmarkTypeExtension extends AbstractBookmarkTypeExtension {

    public static final String BOOKMARK_TYPE_NAME = "shortcut";

    public ShortcutBookmarkTypeExtension() {
        super(BOOKMARK_TYPE_NAME, "Bookmarks for other bookmarks", AllIcons.Actions.Forward);

        addLabelProvider(new ShortcutBookmarkLabelProvider());
        addPropertiesProvider(new ShortcutBookmarkPropertiesProvider(), 10);
        addLocationProvider(new ShortcutBookmarkLocationProvider(), 10);
        addGotoBookmarkHandler(new GotoShortcutBookmark(), 10);
    }
}
