package mesfavoris.snippets.internal;

import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.AbstractBookmarkLabelProvider;
import mesfavoris.icons.MesFavorisIcons;
import mesfavoris.model.Bookmark;
import mesfavoris.snippets.SnippetBookmarkProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SnippetBookmarkLabelProvider extends AbstractBookmarkLabelProvider {

    @Override
    public boolean canHandle(@Nullable Project project, @NotNull Bookmark bookmark) {
        return bookmark.getPropertyValue(SnippetBookmarkProperties.PROP_SNIPPET_CONTENT) != null;
    }

    @Override
    public Icon getIcon(@Nullable Project project, @NotNull Bookmark bookmark) {
        return MesFavorisIcons.snippet;
    }
}
