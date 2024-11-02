package mesfavoris.internal.placeholders;

import mesfavoris.placeholders.IPathPlaceholderResolver;
import mesfavoris.placeholders.PathPlaceholder;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PathPlaceholderResolverTest {
	private final PathPlaceholdersMap mappings = new PathPlaceholdersMap();
	private final IPathPlaceholderResolver pathPlaceholderResolver = new PathPlaceholderResolver(mappings);

	@Test
	public void testCollapse() {
		// Given
		mappings.add(new PathPlaceholder("HOME", Paths.get("/home/cchabanois")));
		mappings.add(new PathPlaceholder("BLT", Paths.get("/home/cchabanois/blt")));

		// When
		String result = pathPlaceholderResolver.collapse(Paths.get("/home/cchabanois/blt/app/main/core"));

		// Then
		assertEquals("${BLT}/app/main/core", result);
	}

	@Test
	public void testCollapseWithGivenPlaceholder() {
		// Given
		mappings.add(new PathPlaceholder("HOME", Paths.get("/home/cchabanois")));
		mappings.add(new PathPlaceholder("BLT", Paths.get("/home/cchabanois/blt")));

		// When
		String result = pathPlaceholderResolver.collapse(Paths.get("/home/cchabanois/blt/app/main/core"), "HOME");

		// Then
		assertEquals("${HOME}/blt/app/main/core", result);
	}	
	
	@Test
	public void testExpand() {
		// Given
		mappings.add(new PathPlaceholder("HOME", Paths.get("/home/cchabanois")));
		mappings.add(new PathPlaceholder("BLT", Paths.get("/home/cchabanois/blt")));

		// When
		Path result = pathPlaceholderResolver.expand("${BLT}/app/main/core");

		// Then
		assertEquals(Paths.get("/home/cchabanois/blt/app/main/core"), result);
	}

	@Test
	public void testExpandNoMatchingPlaceholder() {
		// When
		Path result = pathPlaceholderResolver.expand("${BLT}/app/main/core");

		// Then
		assertNull(result);
	}

	@Test
	public void testExpandAlreadyExpanded() {
		// When
		Path result = pathPlaceholderResolver.expand("/home/cchabanois/blt/app/main/core");

		// Then
		assertEquals(Paths.get("/home/cchabanois/blt/app/main/core"), result);
	}

	@Test
	public void testGetPlaceholderName() {
		// Given
		mappings.add(new PathPlaceholder("HOME", Paths.get("/home/cchabanois")));
		mappings.add(new PathPlaceholder("BLT", Paths.get("/home/cchabanois/blt")));

		// When
		String result = PathPlaceholderResolver.getPlaceholderName("${BLT}/app/main/core");

		// Then
		assertEquals("BLT", result);
	}

}
