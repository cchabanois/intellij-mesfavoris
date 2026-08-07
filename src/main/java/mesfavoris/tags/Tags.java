package mesfavoris.tags;

import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarksTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static mesfavoris.tags.TagsBookmarkProperties.PROP_TAGS;

/**
 * Pure helpers to read, write and query bookmark tags. Tags are stored in the {@code tags} property as a
 * comma-separated string. Tag names cannot contain commas and are normalized to lower case (tags are
 * case-insensitive and always displayed in lower case).
 */
public class Tags {
	private static final String SEPARATOR = ",";

	private Tags() {
	}

	/**
	 * Parses a raw tags string into an ordered, de-duplicated list of lower-case tag names. Blank entries
	 * are dropped and each name is trimmed.
	 */
	public static List<String> parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		Set<String> tags = new LinkedHashSet<>();
		for (String part : raw.split(SEPARATOR)) {
			String tag = part.trim().toLowerCase();
			if (!tag.isEmpty()) {
				tags.add(tag);
			}
		}
		return new ArrayList<>(tags);
	}

	/**
	 * Formats a collection of tags into a canonical, sorted, lower-case comma-separated string. Returns
	 * {@code null} when the result would be empty (so the property gets removed).
	 */
	public static String format(Collection<String> tags) {
		SortedSet<String> sorted = new TreeSet<>();
		for (String tag : tags) {
			if (tag != null) {
				String normalized = tag.trim().toLowerCase();
				if (!normalized.isEmpty()) {
					sorted.add(normalized);
				}
			}
		}
		if (sorted.isEmpty()) {
			return null;
		}
		return String.join(SEPARATOR, sorted);
	}

	/**
	 * Returns the raw string with {@code tag} added (no-op if already present).
	 */
	public static String addTag(String raw, String tag) {
		List<String> tags = new ArrayList<>(parse(raw));
		String normalized = tag == null ? "" : tag.trim().toLowerCase();
		if (!normalized.isEmpty() && !tags.contains(normalized)) {
			tags.add(normalized);
		}
		return format(tags);
	}

	/**
	 * Returns the raw string with {@code tag} removed.
	 */
	public static String removeTag(String raw, String tag) {
		List<String> tags = new ArrayList<>(parse(raw));
		tags.remove(tag == null ? "" : tag.trim().toLowerCase());
		return format(tags);
	}

	/**
	 * Returns the tags of the given bookmark.
	 */
	public static List<String> getTags(Bookmark bookmark) {
		return parse(bookmark.getPropertyValue(PROP_TAGS));
	}

	/**
	 * Collects every distinct tag used across the whole tree, sorted.
	 */
	public static SortedSet<String> collectAllTags(BookmarksTree tree) {
		SortedSet<String> all = new TreeSet<>();
		for (Bookmark bookmark : tree) {
			all.addAll(getTags(bookmark));
		}
		return all;
	}

	/**
	 * Returns true if the bookmark has any tag that contains {@code term} (case-insensitive substring).
	 * A blank term matches any bookmark that has at least one tag.
	 */
	public static boolean hasTagMatching(Bookmark bookmark, String term) {
		String lower = term == null ? "" : term.toLowerCase().trim();
		List<String> tags = getTags(bookmark);
		if (lower.isEmpty()) {
			return !tags.isEmpty();
		}
		return tags.stream().anyMatch(t -> t.contains(lower));
	}

	/**
	 * Extracts the tag search term from a search query if it is a tag query ({@code tag:xxx} or
	 * {@code #xxx}); returns {@code null} otherwise. The returned term is trimmed and lower-cased.
	 */
	public static String extractTagQuery(String searchText) {
		if (searchText == null) {
			return null;
		}
		String trimmed = searchText.trim();
		String lower = trimmed.toLowerCase();
		if (lower.startsWith("tag:")) {
			return trimmed.substring("tag:".length()).trim().toLowerCase();
		}
		if (trimmed.startsWith("#")) {
			return trimmed.substring(1).trim().toLowerCase();
		}
		return null;
	}
}
