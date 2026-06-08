package mesfavoris.url.internal;

import com.google.common.io.ByteStreams;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.sun.net.httpserver.HttpServer;
import mesfavoris.BookmarksDataKeys;
import mesfavoris.model.Bookmark;
import mesfavoris.url.UrlBookmarkProperties;

import javax.imageio.ImageIO;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class UrlBookmarkPropertiesProviderTest extends BasePlatformTestCase {
	private final UrlBookmarkPropertiesProvider provider = new UrlBookmarkPropertiesProvider();
	private HttpServer mockServer;
	private int mockPort;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mockServer = HttpServer.create(new InetSocketAddress(0), 0);
		mockPort = mockServer.getAddress().getPort();

		byte[] pageFavIcon = getResourceBytes("page-favicon.png");
		byte[] favIcon = getResourceBytes("favicon.png");
		mockServer.createContext("/github", exchange -> {
			String html = """
				<html><head>
				  <title>GitHub - cchabanois/intellij-mesfavoris \u00b7 GitHub</title>
				  <link rel="icon" type="image/png" href="/page-favicon.ico">
				</head><body></body></html>
				""";
			byte[] body = html.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		mockServer.createContext("/page-favicon.ico", exchange -> {
			exchange.getResponseHeaders().set("Content-Type", "image/png");
			exchange.sendResponseHeaders(200, pageFavIcon.length);
			exchange.getResponseBody().write(pageFavIcon);
			exchange.close();
		});
		mockServer.createContext("/favicon.ico", exchange -> {
			exchange.getResponseHeaders().set("Content-Type", "image/png");
			exchange.sendResponseHeaders(200, favIcon.length);
			exchange.getResponseBody().write(favIcon);
			exchange.close();
		});
		mockServer.createContext("/auth-required", exchange -> {
			exchange.sendResponseHeaders(401, -1);
			exchange.close();
		});
		mockServer.start();
	}

	@Override
	protected void tearDown() throws Exception {
		mockServer.stop(0);
		super.tearDown();
	}

	public void testUrlBookmarkPropertiesProvider() throws Exception {
		assertTitleAndFavIcon("GitHub - cchabanois/intellij-mesfavoris \u00b7 GitHub", "page-favicon.png",
				"http://localhost:" + mockPort + "/github");
	}

	public void testUrlIsUsedAsTitleWhenAuthenticationIsNeeded() throws IOException {
		String url = "http://localhost:" + mockPort + "/auth-required";
		assertTitleAndFavIcon(url, "favicon.png", url);
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
			byte[] rawBytes = ByteStreams.toByteArray(is);
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(rawBytes));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			return Base64.getEncoder().encodeToString(baos.toByteArray());
		}
	}

	private byte[] getResourceBytes(String resourceName) throws IOException {
		try (InputStream is = getClass().getResourceAsStream(resourceName)) {
			return ByteStreams.toByteArray(is);
		}
	}

}
