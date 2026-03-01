package mesfavoris.intellij.internal;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.intellij.IntellijBookmarkProperties;
import mesfavoris.model.Bookmark;
import org.jetbrains.annotations.Nullable;

public class ActionBookmarkLocationProvider implements IBookmarkLocationProvider {
    @Override
    public @Nullable IBookmarkLocation getBookmarkLocation(Project project, Bookmark bookmark, ProgressIndicator progressIndicator) {
        String actionId = bookmark.getPropertyValue(IntellijBookmarkProperties.PROP_ACTION_ID);
        if (actionId == null || actionId.trim().isEmpty()) {
            return null;
        }
        return new ActionBookmarkLocation(actionId);
    }
}
