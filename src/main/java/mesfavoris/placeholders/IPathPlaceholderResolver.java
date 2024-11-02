package mesfavoris.placeholders;

import java.nio.file.Path;

public interface IPathPlaceholderResolver {

	/**
	 * Expand
	 * 
	 * @param pathWithPlaceholder
	 * @return
	 */
	Path expand(String pathWithPlaceholder);

	String collapse(Path path, String... placeholderNames);

}