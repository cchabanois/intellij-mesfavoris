package mesfavoris.path.internal.resource;

import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class PathDistanceComputerTest {
	private final PathDistanceComputer pathDistanceComputer = new PathDistanceComputer();

	@Test
	public void testNoDistanceBetweenSamePath() {
		assertEquals(0, pathDistanceComputer.distance(Paths.get("/project1/src/package/MyClass.java"),
				Paths.get("/project1/src/package/MyClass.java")));
	}

	@Test
	public void testPathSegmentChanged() {
		assertEquals(1, pathDistanceComputer.distance(Paths.get("/project1/src/packageRenamed/MyClass.java"),
				Paths.get("/project1/src/package/MyClass.java")));
	}

	@Test
	public void testPathSegmentAdded() {
		assertEquals(1, pathDistanceComputer.distance(Paths.get("/project1/src/package/subPackage/MyClass.java"),
				Paths.get("/project1/src/package/MyClass.java")));
	}

	@Test
	public void testPathSegmentRemoved() {
		assertEquals(1, pathDistanceComputer.distance(Paths.get("/project1/src/MyClass.java"),
				Paths.get("/project1/src/package/MyClass.java")));
	}

	@Test
	public void testDifferentFileName() {
		assertEquals(Integer.MAX_VALUE, pathDistanceComputer.distance(
				Paths.get("/project1/src/package/AnotherClass.java"), Paths.get("/project1/src/package/MyClass.java")));
	}

}
