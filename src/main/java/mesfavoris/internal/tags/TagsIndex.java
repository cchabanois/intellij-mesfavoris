package mesfavoris.internal.tags;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import mesfavoris.internal.model.merge.BookmarksTreeIterable;
import mesfavoris.internal.model.merge.BookmarksTreeIterator.Algorithm;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.IBookmarksListener;
import mesfavoris.model.modification.BookmarkDeletedModification;
import mesfavoris.model.modification.BookmarkPropertiesModification;
import mesfavoris.model.modification.BookmarksAddedModification;
import mesfavoris.model.modification.BookmarksModification;
import mesfavoris.service.IBookmarksService;
import mesfavoris.tags.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static mesfavoris.tags.TagsBookmarkProperties.PROP_TAGS;

/**
 * Project-level incremental index of bookmark tags. Instead of scanning the whole tree on every query, it
 * maintains {@code tag -> bookmark ids} in memory and updates it from {@link BookmarksModification} events,
 * so tag lookups stay cheap even with many thousands of bookmarks.
 *
 * <p>The index is built lazily on first access (a single O(N) pass) and then kept in sync. Updates are
 * idempotent per bookmark, so re-delivering or overlapping events cannot corrupt the counts.
 */
@Service(Service.Level.PROJECT)
public final class TagsIndex implements IBookmarksListener, Disposable {
	private final BookmarkDatabase bookmarkDatabase;
	// lowercase tag -> display form (first seen)
	private final Map<String, String> displayByLowerTag = new HashMap<>();
	// lowercase tag -> ids of bookmarks carrying it
	private final Map<String, Set<BookmarkId>> idsByLowerTag = new HashMap<>();
	// bookmark id -> lowercase tags it currently carries (the index's own view, for idempotent updates)
	private final Map<BookmarkId, Set<String>> lowerTagsByBookmark = new HashMap<>();
	private boolean initialized = false;

	@SuppressWarnings("unused") // instantiated by the platform as a project service
	public TagsIndex(@NotNull Project project) {
		this(project.getService(IBookmarksService.class).getBookmarkDatabase());
	}

	public TagsIndex(@NotNull BookmarkDatabase bookmarkDatabase) {
		this.bookmarkDatabase = bookmarkDatabase;
		bookmarkDatabase.addListener(this);
	}

	public static TagsIndex getInstance(@NotNull Project project) {
		return project.getService(TagsIndex.class);
	}

	/**
	 * Returns every distinct tag in use across the tree (display form), ordered case-insensitively.
	 */
	public synchronized SortedSet<String> getAllTags() {
		ensureInitialized();
		SortedSet<String> tags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		tags.addAll(displayByLowerTag.values());
		return tags;
	}

	/**
	 * Returns the ids of the bookmarks carrying the given tag (case-insensitive).
	 */
	public synchronized Set<BookmarkId> getBookmarkIds(String tag) {
		ensureInitialized();
		if (tag == null) {
			return Set.of();
		}
		Set<BookmarkId> ids = idsByLowerTag.get(tag.trim().toLowerCase());
		return ids == null ? Set.of() : new HashSet<>(ids);
	}

	private void ensureInitialized() {
		if (initialized) {
			return;
		}
		for (Bookmark bookmark : bookmarkDatabase.getBookmarksTree()) {
			reindexBookmark(bookmark.getId(), Tags.getTags(bookmark));
		}
		initialized = true;
	}

	@Override
	public synchronized void bookmarksModified(List<BookmarksModification> modifications) {
		if (!initialized) {
			// events before the first read are captured by the initial build
			return;
		}
		for (BookmarksModification modification : modifications) {
			if (modification instanceof BookmarkPropertiesModification propertiesModification) {
				if (touchesTags(propertiesModification)) {
					Bookmark bookmark = propertiesModification.getTargetTree().getBookmark(propertiesModification.getBookmarkId());
					reindexBookmark(propertiesModification.getBookmarkId(), bookmark == null ? List.of() : Tags.getTags(bookmark));
				}
			} else if (modification instanceof BookmarksAddedModification addedModification) {
				for (Bookmark added : addedModification.getBookmarks()) {
					for (Bookmark bookmark : new BookmarksTreeIterable(addedModification.getTargetTree(), added.getId(), Algorithm.PRE_ORDER)) {
						reindexBookmark(bookmark.getId(), Tags.getTags(bookmark));
					}
				}
			} else if (modification instanceof BookmarkDeletedModification deletedModification) {
				for (Bookmark deleted : deletedModification.getDeletedBookmarks()) {
					reindexBookmark(deleted.getId(), List.of());
				}
			}
			// BookmarksMovedModification does not change tags
		}
	}

	private boolean touchesTags(BookmarkPropertiesModification modification) {
		return modification.getAddedProperties().contains(PROP_TAGS)
				|| modification.getModifiedProperties().contains(PROP_TAGS)
				|| modification.getDeletedProperties().contains(PROP_TAGS);
	}

	/**
	 * Sets the tags of a bookmark in the index to exactly {@code newTags}. Idempotent: calling it again with
	 * the same tags is a no-op.
	 */
	private void reindexBookmark(BookmarkId bookmarkId, List<String> newTags) {
		Set<String> newLowerTags = new HashSet<>();
		Map<String, String> displayByLower = new HashMap<>();
		for (String tag : newTags) {
			String lower = tag.toLowerCase();
			if (newLowerTags.add(lower)) {
				displayByLower.put(lower, tag);
			}
		}

		Set<String> oldLowerTags = lowerTagsByBookmark.getOrDefault(bookmarkId, Set.of());
		for (String lower : oldLowerTags) {
			if (!newLowerTags.contains(lower)) {
				removeAssignment(lower, bookmarkId);
			}
		}
		for (String lower : newLowerTags) {
			if (!oldLowerTags.contains(lower)) {
				addAssignment(lower, displayByLower.get(lower), bookmarkId);
			}
		}

		if (newLowerTags.isEmpty()) {
			lowerTagsByBookmark.remove(bookmarkId);
		} else {
			lowerTagsByBookmark.put(bookmarkId, newLowerTags);
		}
	}

	private void addAssignment(String lowerTag, String displayTag, BookmarkId bookmarkId) {
		idsByLowerTag.computeIfAbsent(lowerTag, k -> new HashSet<>()).add(bookmarkId);
		displayByLowerTag.putIfAbsent(lowerTag, displayTag);
	}

	private void removeAssignment(String lowerTag, BookmarkId bookmarkId) {
		Set<BookmarkId> ids = idsByLowerTag.get(lowerTag);
		if (ids != null) {
			ids.remove(bookmarkId);
			if (ids.isEmpty()) {
				idsByLowerTag.remove(lowerTag);
				displayByLowerTag.remove(lowerTag);
			}
		}
	}

	@Override
	public void dispose() {
		bookmarkDatabase.removeListener(this);
	}
}
