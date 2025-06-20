package mesfavoris.internal.snippets;

import mesfavoris.bookmarktype.AbstractBookmarkLabelProvider;
import mesfavoris.icons.MesFavorisIcons;
import mesfavoris.model.Bookmark;

import javax.swing.*;

public class SnippetBookmarkLabelProvider extends AbstractBookmarkLabelProvider {

    @Override
    public boolean canHandle(Context context, Bookmark bookmark) {
        return bookmark.getPropertyValue(SnippetBookmarkProperties.PROP_SNIPPET_CONTENT) != null;
    }

    @Override
    public Icon getIcon(Context context, Bookmark bookmark) {
        return MesFavorisIcons.snippet;
    }
}
