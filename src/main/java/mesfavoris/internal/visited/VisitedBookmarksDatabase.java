package mesfavoris.internal.visited;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import mesfavoris.model.*;
import mesfavoris.model.modification.BookmarkDeletedModification;
import mesfavoris.model.modification.BookmarksModification;
import mesfavoris.topics.BookmarksActivityListener;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class VisitedBookmarksDatabase implements PersistentStateComponent<Element>, IVisitedBookmarksProvider {
	private static final Logger LOG = Logger.getInstance(VisitedBookmarksDatabase.class);
	private static final String NAME_LATEST_VISIT = "latestVisit";
	private static final String NAME_VISIT_COUNT = "visitCount";
	private static final String NAME_BOOKMARK_ID = "bookmarkId";
	private final AtomicReference<VisitedBookmarks> visitedBookmarksMapReference = new AtomicReference<VisitedBookmarks>(
			new VisitedBookmarks());
	private final IBookmarksListener bookmarksListener;
	private final BookmarkDatabase bookmarkDatabase;
	private final Project project;
	private final MessageBusConnection messageBusConnection;

	public VisitedBookmarksDatabase(Project project, BookmarkDatabase bookmarkDatabase) {
		this.project = project;
		this.bookmarkDatabase = bookmarkDatabase;
		bookmarksListener = modifications -> bookmarksDeleted(filterBookmarksDeleteModifications(modifications.stream())
				.flatMap(modification -> StreamSupport.stream(modification.getDeletedBookmarks().spliterator(), false))
				.map(Bookmark::getId).collect(Collectors.toList()));
		messageBusConnection = project.getMessageBus().connect();
	}

	private Stream<BookmarkDeletedModification> filterBookmarksDeleteModifications(
			Stream<BookmarksModification> stream) {
		return stream.filter(modification -> modification instanceof BookmarkDeletedModification)
				.map(modification -> (BookmarkDeletedModification) modification);
	}

	public void init() {
		bookmarkDatabase.addListener(bookmarksListener);
		messageBusConnection.subscribe(BookmarksActivityListener.TOPIC, new BookmarksActivityListener() {
			@Override
			public void bookmarkVisited(BookmarkId bookmarkId) {
				VisitedBookmarksDatabase.this.bookmarkVisited(bookmarkId);
			}
		});
	}

	public void close() {
		bookmarkDatabase.removeListener(bookmarksListener);
		messageBusConnection.disconnect();
	}

	public VisitedBookmarks getVisitedBookmarks() {
		return visitedBookmarksMapReference.get();
	}

	private void bookmarkVisited(BookmarkId bookmarkId) {
		if (bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId) == null) {
			return;
		}
		VisitedBookmarks visitedBookmarksMap;
		VisitedBookmarks newVisitedBookmarksMap;
		do {
			visitedBookmarksMap = visitedBookmarksMapReference.get();
			VisitedBookmark visitedBookmark = visitedBookmarksMap.get(bookmarkId);
			if (visitedBookmark == null) {
				visitedBookmark = new VisitedBookmark(bookmarkId, 1, Instant.now());
			} else {
				visitedBookmark = new VisitedBookmark(bookmarkId, visitedBookmark.visitCount() + 1, Instant.now());
			}
			newVisitedBookmarksMap = visitedBookmarksMap.add(visitedBookmark);
			if (visitedBookmarksMap == newVisitedBookmarksMap) {
				return;
			}
		} while (!visitedBookmarksMapReference.compareAndSet(visitedBookmarksMap, newVisitedBookmarksMap));
		postVisitedBookmarksChanged(visitedBookmarksMap, newVisitedBookmarksMap);
	}

	private void bookmarksDeleted(List<BookmarkId> deletedBookmarkIds) {
		if (deletedBookmarkIds.isEmpty()) {
			return;
		}
		VisitedBookmarks visitedBookmarksMap;
		VisitedBookmarks newVisitedBookmarksMap;
		do {
			visitedBookmarksMap = visitedBookmarksMapReference.get();
			newVisitedBookmarksMap = visitedBookmarksMap;
			for (BookmarkId bookmarkId : deletedBookmarkIds) {
				newVisitedBookmarksMap = newVisitedBookmarksMap.delete(bookmarkId);
			}
			if (visitedBookmarksMap == newVisitedBookmarksMap) {
				return;
			}
		} while (!visitedBookmarksMapReference.compareAndSet(visitedBookmarksMap, newVisitedBookmarksMap));
		postVisitedBookmarksChanged(visitedBookmarksMap, newVisitedBookmarksMap);
	}

	private void postVisitedBookmarksChanged(VisitedBookmarks visitedBookmarksMap,
			VisitedBookmarks newVisitedBookmarksMap) {
		project.getMessageBus().syncPublisher(VisitedBookmarksListener.TOPIC)
				.visitedBookmarksChanged(visitedBookmarksMap, newVisitedBookmarksMap);
	}

	@Nullable
	@Override
	public Element getState() {
		VisitedBookmarks visitedBookmarks = visitedBookmarksMapReference.get();
		Element container = new Element("VisitedBookmarks");

		for (VisitedBookmark visitedBookmark : visitedBookmarks.getSet()) {
			Element bookmarkElement = new Element("bookmark");
			bookmarkElement.setAttribute(NAME_BOOKMARK_ID, visitedBookmark.bookmarkId().toString());
			bookmarkElement.setAttribute(NAME_VISIT_COUNT, String.valueOf(visitedBookmark.visitCount()));
			bookmarkElement.setAttribute(NAME_LATEST_VISIT, visitedBookmark.latestVisit().toString());
			container.addContent(bookmarkElement);
		}

		return container;
	}

	@Override
	public void loadState(@NotNull Element state) {
		BookmarksTree bookmarksTree = bookmarkDatabase.getBookmarksTree();
		VisitedBookmarks visitedBookmarks = new VisitedBookmarks();

		for (Element bookmarkElement : state.getChildren("bookmark")) {
			String bookmarkIdString = bookmarkElement.getAttributeValue(NAME_BOOKMARK_ID);
			String visitCountString = bookmarkElement.getAttributeValue(NAME_VISIT_COUNT);
			String latestVisitString = bookmarkElement.getAttributeValue(NAME_LATEST_VISIT);

			if (bookmarkIdString != null && visitCountString != null && latestVisitString != null) {
				try {
					BookmarkId bookmarkId = new BookmarkId(bookmarkIdString);
					int visitCount = Integer.parseInt(visitCountString);
					Instant latestVisit = Instant.parse(latestVisitString);

					if (bookmarksTree.getBookmark(bookmarkId) != null) {
						VisitedBookmark visitedBookmark = new VisitedBookmark(bookmarkId, visitCount, latestVisit);
						visitedBookmarks = visitedBookmarks.add(visitedBookmark);
					}
				} catch (DateTimeParseException | NumberFormatException e) {
					LOG.warn("Could not parse visited bookmark data: " + e.getMessage());
				}
			}
		}

		visitedBookmarksMapReference.set(visitedBookmarks);
	}
}
