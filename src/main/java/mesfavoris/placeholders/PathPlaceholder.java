package mesfavoris.placeholders;

import java.nio.file.Path;

public class PathPlaceholder {
	private final String name;
	private final Path path;
	
	public PathPlaceholder(String name, Path path) {
		this.name = name;
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
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PathPlaceholder [name=" + name + ", path=" + path + "]";
	}	
	
}