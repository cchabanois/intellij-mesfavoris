package mesfavoris.snippets.internal;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.snippets.SnippetBookmarkProperties;

public class SnippetBookmarkLocationProvider implements IBookmarkLocationProvider {
    @Override
    public IBookmarkLocation getBookmarkLocation(Project project, Bookmark bookmark, ProgressIndicator progress) {
        String snippetContent = bookmark.getPropertyValue(SnippetBookmarkProperties.PROP_SNIPPET_CONTENT);
        if (snippetContent == null) {
            return null;
        }
        return new Snippet(snippetContent);
    }
}
