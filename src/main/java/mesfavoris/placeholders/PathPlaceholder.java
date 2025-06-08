package mesfavoris.placeholders;

import java.nio.file.Path;

public class PathPlaceholder {
	private final String name;
	private final Path path;
	
	public PathPlaceholder(String name, Path path) {
		if (name == null) {
			throw new IllegalArgumentException("Placeholder name cannot be null");
		}
		if (path == null) {
			throw new IllegalArgumentException("Placeholder path cannot be null");
		}
		this.name = name.toUpperCase();
		this.path = path.toAbsolutePath();
	}
	
	public String getName() {
		return name;
	}
	
	public Path getPath() {
		return path;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + name.hashCode();
		result = prime * result + path.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PathPlaceholder other = (PathPlaceholder) obj;
		return name.equals(other.name) && path.equals(other.path);
    }

	@Override
	public String toString() {
		return "PathPlaceholder [name=" + name + ", path=" + path + "]";
	}	
	
}
