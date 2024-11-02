package mesfavoris.path.internal.resource;

import mesfavoris.path.resource.IPathDistanceComputer;

import java.nio.file.Path;

/**
 * Compute a distance between two paths with same last segment
 * 
 * @author cchabanois
 *
 */
public class PathDistanceComputer implements IPathDistanceComputer {

	@Override
	public int distance(Path path1, Path path2) {
		if (path1.getNameCount() == 0 || path2.getNameCount() == 0) {
			return Integer.MAX_VALUE;
		}
		if (!path1.getFileName().equals(path2.getFileName())) {
			return Integer.MAX_VALUE;
		}
		int distance = 0;
		String[] path1Segments = segments(path1);
		String[] path2Segments = segments(path2);
		distance += levenshteinDistance(path1Segments, path2Segments);
		return distance;
	}

	private String[] segments(Path path) {
		int nameCount = path.getNameCount();
		String[] segments = new String[nameCount];
		for (int i = 0; i < nameCount; i++) {
			segments[i] = path.getName(i).toString();
		}
		return segments;
	}

	private int levenshteinDistance(String[] path1Segments, String[] path2Segments) {
		int len0 = path1Segments.length + 1;
		int len1 = path2Segments.length + 1;
		int[] cost = new int[len0];
		int[] newCost = new int[len0];
		for (int i = 0; i < len0; i++) {
			cost[i] = i;
		}
		for (int j = 1; j < len1; j++) {
			newCost[0] = j;
			for (int i = 1; i < len0; i++) {
				int match = (path1Segments[i - 1].equals(path2Segments[j - 1])) ? 0 : 1;

				// computing cost for each transformation
				int costReplace = cost[i - 1] + match;
				int costInsert = cost[i] + 1;
				int costDelete = newCost[i - 1] + 1;

				// keep minimum cost
				newCost[i] = Math.min(Math.min(costInsert, costDelete), costReplace);
			}

			// swap cost/newcost arrays
			int[] swap = cost;
			cost = newCost;
			newCost = swap;
		}
		return cost[len0 - 1];
	}

}
