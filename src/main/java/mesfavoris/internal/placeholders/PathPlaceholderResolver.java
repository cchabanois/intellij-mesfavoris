package mesfavoris.internal.placeholders;

import mesfavoris.placeholders.IPathPlaceholderResolver;
import mesfavoris.placeholders.IPathPlaceholders;
import mesfavoris.placeholders.PathPlaceholder;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class PathPlaceholderResolver implements IPathPlaceholderResolver {
	private final IPathPlaceholders mappings;

	public PathPlaceholderResolver(IPathPlaceholders mappings) {
		this.mappings = mappings;
	}

	/**
	 * Expand
	 * 
	 * @param pathWithPlaceholder
	 * @return
	 */
	@Override
	public Path expand(String pathWithPlaceholder) {
		String variableName = getPlaceholderName(pathWithPlaceholder);
		if (variableName == null) {
			try {
				return Paths.get(pathWithPlaceholder);
			} catch (InvalidPathException e) {
				return null;
			}
		}
		String other = pathWithPlaceholder.substring(variableName.length() + 4);
		PathPlaceholder pathPlaceholder = mappings.get(variableName);
		if (pathPlaceholder == null || pathPlaceholder.getPath() == null) {
			return null;
		}
		return pathPlaceholder.getPath().resolve(other);
	}

	public static String getPlaceholderName(String pathWithPlaceholder) {
		int index1 = pathWithPlaceholder.indexOf("${");
		int index2 = pathWithPlaceholder.indexOf("}/");
		if (index1 != 0 || index2 == -1) {
			return null;
		}
		String variableName = pathWithPlaceholder.substring(2, index2);
		return variableName;
	}

	@Override
	public String collapse(Path path, String... placeholderNames) {
		path = path.toAbsolutePath();
		String bestCollapsedPath = null;
		int bestScore = Integer.MAX_VALUE;

		Iterable<PathPlaceholder> pathPlaceholders;
		if (placeholderNames.length == 0) {
			pathPlaceholders = mappings;
		} else {
			pathPlaceholders = Arrays.stream(placeholderNames).map(mappings::get).filter(Objects::nonNull)
					.collect(Collectors.toList());
		}

		for (PathPlaceholder pathPlaceholder : pathPlaceholders) {
			String variable = pathPlaceholder.getName();
			Path variablePath = pathPlaceholder.getPath();
			if (variablePath != null && path.startsWith(variablePath)) {
				Path relativePath = variablePath.relativize(path);
				int score = relativePath.toString().length();
				if (score < bestScore) {
					bestCollapsedPath = "${" + variable + "}/" + relativePath;
					bestScore = score;
				}
			}
		}
		if (bestCollapsedPath == null) {
			bestCollapsedPath = path.toString();
		}
		return bestCollapsedPath;
	}

}
