package mesfavoris.tests.commons.markers;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.PlatformTestUtil;
import mesfavoris.internal.markers.highlighters.BookmarksHighlightersUtils;
import mesfavoris.model.BookmarkId;
import mesfavoris.tests.commons.waits.Waiter;

import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Test utilities for bookmark highlighters
 */
public class BookmarkHighlightersTestUtils {

    /**
     * Wait until a bookmark highlighter appears at the specified line
     *
     * @param project the project
     * @param document the document
     * @param bookmarkId the bookmark ID to wait for
     * @param lineNumber the expected line number
     * @throws TimeoutException if the highlighter doesn't appear within the timeout
     */
    public static void waitUntilBookmarkHighlighterAtLine(Project project, Document document, BookmarkId bookmarkId, int lineNumber) throws TimeoutException {
        Waiter.waitUntil("No bookmark highlighter found or highlighter not at expected line", () -> {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            List<BookmarkId> bookmarkIds = BookmarksHighlightersUtils.findBookmarksAtLine(project, document, lineNumber);
            return bookmarkIds.contains(bookmarkId);
        });
    }
}

