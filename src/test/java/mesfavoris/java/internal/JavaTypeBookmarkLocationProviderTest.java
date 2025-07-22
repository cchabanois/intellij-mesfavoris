package mesfavoris.java.internal;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;

import static mesfavoris.java.JavaBookmarkProperties.*;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_LINE_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaTypeBookmarkLocationProviderTest extends BasePlatformTestCase {

	private JavaTypeMemberBookmarkLocationProvider javaBookmarkLocationProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// Copy test data to project
		myFixture.copyDirectoryToProject("commons-cli", "commons-cli");

		javaBookmarkLocationProvider = new JavaTypeMemberBookmarkLocationProvider();
	}

	@Override
	protected String getTestDataPath() {
		return "src/test/testData";
	}

	public void testJavaTypeFindLocation() {
		// Given
		Bookmark bookmark = new Bookmark(new BookmarkId(),
				ImmutableMap.of(PROP_JAVA_TYPE, "org.apache.commons.cli.DefaultParser"));

		// When
		JavaTypeMemberBookmarkLocation location = javaBookmarkLocationProvider.getBookmarkLocation(
				getProject(), bookmark, new EmptyProgressIndicator());

		// Then
		assertThat(location).isNotNull();
		assertThat(location.getMember().getName()).isEqualTo("DefaultParser");
		assertThat(location.getLineNumber()).isEqualTo(30);
	}

	public void testJavaMethodFindLocation() {
		// Given
		Bookmark bookmark = new Bookmark(new BookmarkId(),
				ImmutableMap.of(PROP_JAVA_DECLARING_TYPE, "org.apache.commons.cli.DefaultParser",
						PROP_JAVA_ELEMENT_KIND, KIND_METHOD, PROP_JAVA_ELEMENT_NAME, "parse"));

		// When
		JavaTypeMemberBookmarkLocation location = javaBookmarkLocationProvider.getBookmarkLocation(
				getProject(), bookmark, new EmptyProgressIndicator());

		// Then
		// there are several parse methods but we don't have enough information
		// to choose : we get the first one
		assertThat(location).isNotNull();
		assertThat(location.getMember().getName()).isEqualTo("parse");
		assertThat(location.getLineNumber()).isEqualTo(57);
	}

	public void testJavaMethodFindLocationUsingSignature() {
		// Given
		Bookmark bookmark = new Bookmark(new BookmarkId(),
				ImmutableMap.of(PROP_JAVA_DECLARING_TYPE, "org.apache.commons.cli.DefaultParser",
						PROP_JAVA_ELEMENT_KIND, KIND_METHOD, PROP_JAVA_ELEMENT_NAME, "parse",
						PROP_JAVA_METHOD_SIGNATURE, "CommandLine parse(Options,String[],Properties,boolean)"));

		// When
		JavaTypeMemberBookmarkLocation location = javaBookmarkLocationProvider.getBookmarkLocation(
				getProject(), bookmark, new EmptyProgressIndicator());

		// Then
		assertThat(location).isNotNull();
		assertThat(location.getMember().getName()).isEqualTo("parse");
		assertThat(location.getLineNumber()).isEqualTo(98);
	}

	public void testInsideJavaElementFindLocation() {
		// Given
		Bookmark bookmark = new Bookmark(new BookmarkId(),
				ImmutableMap.of(PROP_JAVA_DECLARING_TYPE, "org.apache.commons.cli.DefaultParser",
						PROP_JAVA_ELEMENT_KIND, KIND_METHOD, PROP_JAVA_ELEMENT_NAME, "handleProperties",
						PROP_LINE_NUMBER_INSIDE_ELEMENT, "7"));

		// When
		JavaTypeMemberBookmarkLocation location = javaBookmarkLocationProvider.getBookmarkLocation(
				getProject(), bookmark, new EmptyProgressIndicator());

		// Then
		assertThat(location).isNotNull();
		assertThat(location.getMember().getName()).isEqualTo("handleProperties");
		assertThat(location.getLineNumber()).isEqualTo(146);
	}

	public void testInsideJavaElementFuzzyFindLocation() {
		// Given
		Bookmark bookmark = new Bookmark(new BookmarkId(), ImmutableMap.of(PROP_JAVA_DECLARING_TYPE,
				"org.apache.commons.cli.DefaultParser", PROP_JAVA_ELEMENT_KIND, KIND_METHOD, PROP_JAVA_ELEMENT_NAME,
				"handleProperties", PROP_LINE_CONTENT,
				"for (Enumeration<?> enumeration = properties.propertyNames(); enumeration.hasMoreElements();)"));

		// When
		JavaTypeMemberBookmarkLocation location = javaBookmarkLocationProvider.getBookmarkLocation(
				getProject(), bookmark, new EmptyProgressIndicator());

		// Then
		assertThat(location).isNotNull();
		assertThat(location.getMember().getName()).isEqualTo("handleProperties");
		assertThat(location.getLineNumber()).isEqualTo(146);
	}

}
