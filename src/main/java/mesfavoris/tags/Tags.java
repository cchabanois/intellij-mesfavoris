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
 * comma-separated string. Tag names cannot contain commas and are compared case-insensitively (their
 * display casing is preserved).
 */
public class Tags {
	private static final String SEPARATOR = ",";

	private Tags() {
	}

	/**
	 * Parses a raw tags string into an ordered, de-duplicated (case-insensitive) list of tag names.
	 * Blank entries are dropped and each name is trimmed.
	 */
	public static List<String> parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		Set<String> lowerSeen = new LinkedHashSet<>();
		List<String> result = new ArrayList<>();
		for (String part : raw.split(SEPARATOR)) {
			String tag = part.trim();
			if (tag.isEmpty()) {
				continue;
			}
			if (lowerSeen.add(tag.toLowerCase())) {
				result.add(tag);
			}
		}
		return result;
	}

	/**
	 * Formats a collection of tags into a canonical, case-insensitively sorted, comma-separated string.
	 * Returns {@code null} when the result would be empty (so the property gets removed).
	 */
	public static String format(Collection<String> tags) {
		SortedSet<String> sorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (String tag : tags) {
			if (tag != null && !tag.trim().isEmpty()) {
				sorted.add(tag.trim());
			}
		}
		if (sorted.isEmpty()) {
			return null;
		}
		return String.join(SEPARATOR, sorted);
	}

	/**
	 * Returns the raw string with {@code tag} added (no-op if already present, case-insensitively).
	 */
	public static String addTag(String raw, String tag) {
		List<String> tags = new ArrayList<>(parse(raw));
		String trimmed = tag == null ? "" : tag.trim();
		if (!trimmed.isEmpty() && tags.stream().noneMatch(t -> t.equalsIgnoreCase(trimmed))) {
			tags.add(trimmed);
		}
		return format(tags);
	}

	/**
	 * Returns the raw string with {@code tag} removed (case-insensitively).
	 */
	public static String removeTag(String raw, String tag) {
		List<String> tags = new ArrayList<>(parse(raw));
		String trimmed = tag == null ? "" : tag.trim();
		tags.removeIf(t -> t.equalsIgnoreCase(trimmed));
		return format(tags);
	}

	/**
	 * Returns the tags of the given bookmark.
	 */
	public static List<String> getTags(Bookmark bookmark) {
		return parse(bookmark.getPropertyValue(PROP_TAGS));
	}

	/**
	 * Collects every distinct tag used across the whole tree, ordered case-insensitively.
	 */
	public static SortedSet<String> collectAllTags(BookmarksTree tree) {
		SortedSet<String> all = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
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
		return tags.stream().anyMatch(t -> t.toLowerCase().contains(lower));
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
