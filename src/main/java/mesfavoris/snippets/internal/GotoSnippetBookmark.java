package mesfavoris.snippets.internal;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IGotoBookmark;
import mesfavoris.model.Bookmark;

public class GotoSnippetBookmark implements IGotoBookmark  {

    @Override
    public boolean gotoBookmark(Project project, Bookmark bookmark, IBookmarkLocation bookmarkLocation) {
        if (!(bookmarkLocation instanceof Snippet snippet)) {
            return false;
        }
        CopyPasteManager.copyTextToClipboard(snippet.getContent());

        // Notify user that snippet was copied to clipboard
        Notification notification = new Notification(
                "com.cchabanois.mesfavoris.info",
                "Snippet copied to clipboard",
                "The snippet content has been copied to your clipboard",
                NotificationType.INFORMATION
        );
        Notifications.Bus.notify(notification, project);

        return true;
    }

}
