package mesfavoris.recent;

import mesfavoris.internal.recent.RecentBookmarks;

/**
 * Provider for accessing recent bookmarks.
 * This interface provides read-only access to recent bookmarks without exposing lifecycle methods.
 */
public interface IRecentBookmarksProvider {

    /**
     * Get the recent bookmarks
     *
     * @return the recent bookmarks
     */
    RecentBookmarks getRecentBookmarks();
}

