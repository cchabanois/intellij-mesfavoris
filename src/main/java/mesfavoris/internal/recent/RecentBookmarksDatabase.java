package mesfavoris.internal.recent;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import mesfavoris.model.*;
import mesfavoris.model.modification.BookmarkDeletedModification;
import mesfavoris.model.modification.BookmarksAddedModification;
import mesfavoris.model.modification.BookmarksModification;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Database for tracking recently added bookmarks.
 * Persistence is managed by BookmarksService.
 */
public class RecentBookmarksDatabase implements PersistentStateComponent<Element> {
	private static final Logger LOG = Logger.getInstance(RecentBookmarksDatabase.class);
	private static final String NAME_DATE = "date";
	private static final String NAME_BOOKMARK_ID = "bookmarkId";

	private final Project project;
	private final AtomicReference<RecentBookmarks> recentBookmarksReference;
	private final BookmarkDatabase bookmarkDatabase;
	private final IBookmarksListener bookmarksListener;

	public RecentBookmarksDatabase(Project project, BookmarkDatabase bookmarkDatabase, Duration recentDuration) {
		this.project = project;
		this.bookmarkDatabase = bookmarkDatabase;
		bookmarksListener = modifications -> bookmarksAddedOrDeleted(getAddedBookmarks(modifications), getDeletedBookmarks(modifications));
		recentBookmarksReference = new AtomicReference<>(new RecentBookmarks(recentDuration));
	}

	public void init() {
		bookmarkDatabase.addListener(bookmarksListener);
	}

	public void close() {
		bookmarkDatabase.removeListener(bookmarksListener);
	}

	public RecentBookmarks getRecentBookmarks() {
		return recentBookmarksReference.get();
	}

	private void bookmarksAddedOrDeleted(List<BookmarkId> addedBookmarkIds, List<BookmarkId> deletedBookmarkIds) {
		if (deletedBookmarkIds.isEmpty() && addedBookmarkIds.isEmpty()) {
			return;
		}
		BookmarksTree bookmarksTree = bookmarkDatabase.getBookmarksTree();
		RecentBookmarks recentBookmarks;
		RecentBookmarks newRecentBookmarks;
		do {
			recentBookmarks = recentBookmarksReference.get();
			newRecentBookmarks = recentBookmarks;
			for (BookmarkId bookmarkId : addedBookmarkIds) {
				Optional<RecentBookmark> recentBookmark = createRecentBookmark(bookmarksTree, bookmarkId);
				if (recentBookmark.isPresent()) {
					newRecentBookmarks = newRecentBookmarks.add(recentBookmark.get());
				}
			}
			for (BookmarkId bookmarkId : deletedBookmarkIds) {
				newRecentBookmarks = newRecentBookmarks.delete(bookmarkId);
			}
			if (recentBookmarks == newRecentBookmarks) {
				return;
			}
		} while (!recentBookmarksReference.compareAndSet(recentBookmarks, newRecentBookmarks));
		postRecentBookmarksChanged(recentBookmarks, newRecentBookmarks);
	}

	private Optional<RecentBookmark> createRecentBookmark(BookmarksTree bookmarksTree, BookmarkId bookmarkId) {
		Bookmark bookmark = bookmarksTree.getBookmark(bookmarkId);
		if (bookmark == null || bookmark.getPropertyValue(Bookmark.PROPERTY_CREATED) == null) {
			return Optional.empty();
		}
		try {
			Instant instantAdded = Instant.parse(bookmark.getPropertyValue(Bookmark.PROPERTY_CREATED));
			return Optional.of(new RecentBookmark(bookmarkId, instantAdded));
		} catch (DateTimeParseException e) {
			return Optional.empty();
		}
	}

	private void postRecentBookmarksChanged(RecentBookmarks previousRecentBookmarks,
			RecentBookmarks newRecentBookmarks) {
		project.getMessageBus().syncPublisher(RecentBookmarksListener.TOPIC)
				.recentBookmarksChanged(previousRecentBookmarks, newRecentBookmarks);
	}

	@Nullable
	@Override
	public Element getState() {
		RecentBookmarks recentBookmarks = recentBookmarksReference.get();
		Element container = new Element("RecentBookmarks");

		for (RecentBookmark recentBookmark : recentBookmarks.getSet()) {
			Element bookmarkElement = new Element("bookmark");
			bookmarkElement.setAttribute(NAME_BOOKMARK_ID, recentBookmark.getBookmarkId().toString());
			bookmarkElement.setAttribute(NAME_DATE, recentBookmark.getInstantAdded().toString());
			container.addContent(bookmarkElement);
		}

		return container;
	}

	@Override
	public void loadState(@NotNull Element state) {
		BookmarksTree bookmarksTree = bookmarkDatabase.getBookmarksTree();
		RecentBookmarks recentBookmarks = new RecentBookmarks(recentBookmarksReference.get().getRecentDuration());

		for (Element bookmarkElement : state.getChildren("bookmark")) {
			String bookmarkIdString = bookmarkElement.getAttributeValue(NAME_BOOKMARK_ID);
			String dateString = bookmarkElement.getAttributeValue(NAME_DATE);

			if (bookmarkIdString != null && dateString != null) {
				try {
					BookmarkId bookmarkId = new BookmarkId(bookmarkIdString);
					Instant instantAdded = Instant.parse(dateString);

					if (bookmarksTree.getBookmark(bookmarkId) != null) {
						RecentBookmark recentBookmark = new RecentBookmark(bookmarkId, instantAdded);
						recentBookmarks = recentBookmarks.add(recentBookmark);
					}
				} catch (DateTimeParseException e) {
					LOG.warn("Could not parse date for recent bookmark: " + dateString, e);
				}
			}
		}

		recentBookmarksReference.set(recentBookmarks);
	}

	private List<BookmarkId> getDeletedBookmarks(List<BookmarksModification> modifications) {
		return modifications.stream()
				.filter(modification -> modification instanceof BookmarkDeletedModification)
				.map(modification -> (BookmarkDeletedModification) modification)
				.flatMap(modification -> StreamSupport.stream(modification.getDeletedBookmarks().spliterator(), false))
				.map(Bookmark::getId)
				.collect(Collectors.toList());
	}

	private List<BookmarkId> getAddedBookmarks(List<BookmarksModification> modifications) {
		return modifications.stream()
				.filter(modification -> modification instanceof BookmarksAddedModification)
				.map(modification -> (BookmarksAddedModification) modification)
				.flatMap(modification -> modification.getBookmarks().stream())
				.map(Bookmark::getId)
				.collect(Collectors.toList());
	}

}
