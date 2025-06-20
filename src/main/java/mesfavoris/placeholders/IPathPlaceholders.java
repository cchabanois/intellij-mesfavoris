package mesfavoris.placeholders;

public interface IPathPlaceholders extends Iterable<PathPlaceholder> {
	String PLACEHOLDER_HOME_NAME = "HOME";

	PathPlaceholder get(String name);
	
}
