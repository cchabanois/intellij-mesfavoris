package mesfavoris.url.internal;

import com.google.common.io.ByteStreams;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.BookmarksDataKeys;
import mesfavoris.model.Bookmark;
import mesfavoris.url.UrlBookmarkProperties;

import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class UrlBookmarkPropertiesProviderTest extends BasePlatformTestCase {
	private final UrlBookmarkPropertiesProvider provider = new UrlBookmarkPropertiesProvider();

	public void testUrlBookmarkPropertiesProvider() throws Exception {
		assertTitleAndFavIcon("cchabanois/intellij-mesfavoris", "github-32x32.png",
				"https://github.com/cchabanois/intellij-mesfavoris");
	}

	public void testUrlIsUsedAsTitleWhenAuthenticationIsNeeded() throws IOException {
		String url = "https://docs.google.com/a/salesforce.com/file/d/0B97G1IRAgxIEanhJTmkyS0NFem8/edit";
		assertTitleAndFavIcon(url, "google-32x32.png", url);
	}

	private void assertTitleAndFavIcon(String expectedTitle, String expectedIcon, String url) throws IOException {
		StringSelection transferable = new StringSelection(url);
		DataContext dataContext = SimpleDataContext.builder()
			.add(BookmarksDataKeys.TRANSFERABLE_DATA_KEY, transferable)
			.build();
		Map<String, String> bookmarkProperties = new HashMap<>();
		provider.addBookmarkProperties(bookmarkProperties, dataContext, new EmptyProgressIndicator());
		assertEquals(expectedTitle, bookmarkProperties.get(Bookmark.PROPERTY_NAME));
		// Can be used to update the expected file
//		Files.write(Paths.get(expectedIcon), Base64.getDecoder().decode(bookmarkProperties.get(UrlBookmarkProperties.PROP_ICON)));
		assertEquals(expectedIcon == null ? null : getImageAsIconBase64(expectedIcon),
				bookmarkProperties.get(UrlBookmarkProperties.PROP_ICON));
	}

	private String getImageAsIconBase64(String resourceName) throws IOException {
		try (InputStream is = getClass().getResourceAsStream(resourceName)) {
			byte[] byteArray = ByteStreams.toByteArray(is);
			return Base64.getEncoder().encodeToString(byteArray);
		}
	}

}
