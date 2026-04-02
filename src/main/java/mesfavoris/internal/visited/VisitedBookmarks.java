package mesfavoris.internal.visited;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.util.JImmutables;

import mesfavoris.model.BookmarkId;

public class VisitedBookmarks {
	private final JImmutableSet<VisitedBookmark> byVisitCountSet;
	private final JImmutableSet<VisitedBookmark> byLatestVisitSet;
	private final JImmutableMap<BookmarkId, VisitedBookmark> map;

	public VisitedBookmarks() {
		Comparator<VisitedBookmark> comparator = (b1, b2) -> {
			int result = b2.visitCount() - b1.visitCount();
			if (result == 0) {
				return b2.bookmarkId().toString().compareTo(b1.bookmarkId().toString());
			} else {
				return result;
			}
		};
		byVisitCountSet = JImmutables.sortedSet(comparator);
		comparator = (b1, b2) -> {
			int result = b2.latestVisit().compareTo(b1.latestVisit());
			if (result == 0) {
				return b2.bookmarkId().toString().compareTo(b1.bookmarkId().toString());
			} else {
				return result;
			}
		};
		byLatestVisitSet = JImmutables.sortedSet(comparator);
		map = JImmutables.map();
	}

	public List<BookmarkId> getMostVisitedBookmarks(int count) {
		return byVisitCountSet.getSet().stream().map(VisitedBookmark::bookmarkId).limit(count)
				.collect(Collectors.toList());
	}

	public List<BookmarkId> getLatestVisitedBookmarks(int count) {
		return byLatestVisitSet.getSet().stream().map(VisitedBookmark::bookmarkId).limit(count)
				.collect(Collectors.toList());
	}
	
	private VisitedBookmarks(JImmutableSet<VisitedBookmark> byVisitCountSet,
			JImmutableSet<VisitedBookmark> byLatestVisitSet, JImmutableMap<BookmarkId, VisitedBookmark> map) {
		this.byVisitCountSet = byVisitCountSet;
		this.byLatestVisitSet = byLatestVisitSet;
		this.map = map;
	}

	public VisitedBookmark get(BookmarkId bookmarkId) {
		return map.get(bookmarkId);
	}

	public Set<VisitedBookmark> getSet() {
		return byVisitCountSet.getSet();
	}

	int size() {
		return byVisitCountSet.size();
	}

	public VisitedBookmarks add(VisitedBookmark visitedBookmark) {
		JImmutableMap<BookmarkId, VisitedBookmark> newMap = map;
		JImmutableSet<VisitedBookmark> newByVisitCountSet = byVisitCountSet;
		JImmutableSet<VisitedBookmark> newByLatestVisitSet = byLatestVisitSet;
		VisitedBookmark oldValue = newMap.get(visitedBookmark.bookmarkId());
		newMap = newMap.assign(visitedBookmark.bookmarkId(), visitedBookmark);
		if (oldValue != null) {
			newByVisitCountSet = newByVisitCountSet.delete(oldValue);
			newByLatestVisitSet = newByLatestVisitSet.delete(oldValue);
		}
		newByVisitCountSet = newByVisitCountSet.insert(visitedBookmark);
		newByLatestVisitSet = newByLatestVisitSet.insert(visitedBookmark);
		return newVisitedBookmarks(newByVisitCountSet, newByLatestVisitSet, newMap);
	}

	private VisitedBookmarks newVisitedBookmarks(JImmutableSet<VisitedBookmark> newByVisitCountSet,
			JImmutableSet<VisitedBookmark> newByLatestVisitSet, JImmutableMap<BookmarkId, VisitedBookmark> newMap) {
		if (newMap == map && newByVisitCountSet == byVisitCountSet && newByLatestVisitSet == byLatestVisitSet) {
			return this;
		} else {
			return new VisitedBookmarks(newByVisitCountSet, newByLatestVisitSet, newMap);
		}
	}

	public VisitedBookmarks delete(BookmarkId bookmarkId) {
		VisitedBookmark visitedBookmark = get(bookmarkId);
		if (visitedBookmark == null) {
			return this;
		}
		JImmutableMap<BookmarkId, VisitedBookmark> newMap = map;
		JImmutableSet<VisitedBookmark> newByVisitCountSet = byVisitCountSet;
		JImmutableSet<VisitedBookmark> newByLatestVisitSet = byLatestVisitSet;
		newMap = newMap.delete(bookmarkId);
		newByVisitCountSet = newByVisitCountSet.delete(visitedBookmark);
		newByLatestVisitSet = newByLatestVisitSet.delete(visitedBookmark);
		return newVisitedBookmarks(newByVisitCountSet, newByLatestVisitSet, newMap);
	}

}