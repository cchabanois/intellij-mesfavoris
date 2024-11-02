package mesfavoris.path.resource;

import java.nio.file.Path;

public interface IPathDistanceComputer {
    int distance(Path path1, Path path2);
}
