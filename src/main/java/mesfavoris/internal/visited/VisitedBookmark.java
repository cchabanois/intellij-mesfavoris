package mesfavoris.internal.visited;

import java.time.Instant;

import mesfavoris.model.BookmarkId;

public record VisitedBookmark(BookmarkId bookmarkId, int visitCount, Instant latestVisit) {

}