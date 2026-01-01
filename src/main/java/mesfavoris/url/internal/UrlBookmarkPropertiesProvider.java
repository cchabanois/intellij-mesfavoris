package mesfavoris.url.internal;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.BookmarksDataKeys;
import mesfavoris.bookmarktype.AbstractBookmarkPropertiesProvider;
import mesfavoris.commons.SubProgressIndicator;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static mesfavoris.model.Bookmark.PROPERTY_NAME;
import static mesfavoris.url.UrlBookmarkProperties.PROP_ICON;
import static mesfavoris.url.UrlBookmarkProperties.PROP_URL;

public class UrlBookmarkPropertiesProvider extends AbstractBookmarkPropertiesProvider {

	private static final int TARGET_ICON_SIZE = 32;
	private static final String PNG_FORMAT_NAME = "png";
	private static final int MAX_BODY_SIZE = 32768;

	public UrlBookmarkPropertiesProvider() {

	}

	@Override
	public void addBookmarkProperties(Map<String, String> bookmarkProperties, DataContext dataContext, ProgressIndicator progress) {
		Transferable transferable = dataContext.getData(BookmarksDataKeys.TRANSFERABLE_DATA_KEY);
		if (transferable == null) {
			return;
		}
        URL url = getURL(transferable);
		if (url == null) {
			return;
		}
		putIfAbsent(bookmarkProperties, PROP_URL, url.toString());
		if (!isPresent(bookmarkProperties, PROPERTY_NAME) || !isPresent(bookmarkProperties, PROP_ICON)) {
			Optional<Document> document;
			try(SubProgressIndicator subProgressIndicator = new SubProgressIndicator(progress, 0.5)) {
				document = parse(url, subProgressIndicator);
			}
			if (document.isPresent()) {
				getTitle(document.get()).ifPresent(title -> putIfAbsent(bookmarkProperties, PROPERTY_NAME, title));
				try(SubProgressIndicator subProgressIndicator = new SubProgressIndicator(progress, 0.5)) {
					getFavIconAsBase64(url, document.get(), subProgressIndicator)
							.ifPresent(favIcon -> putIfAbsent(bookmarkProperties, PROP_ICON, favIcon));
				}
			} else {
				try(SubProgressIndicator subProgressIndicator = new SubProgressIndicator(progress, 0.5)) {
					getFavIconUrl(url).flatMap(u -> getFavIconAsBase64(u, subProgressIndicator))
							.ifPresent(favIcon -> putIfAbsent(bookmarkProperties, PROP_ICON, favIcon));
				}
			}
		}
		putIfAbsent(bookmarkProperties, PROPERTY_NAME, url.toString());
	}

	private URL getURL(Transferable transferable) {
		try {
			String text = (String) transferable.getTransferData(DataFlavor.stringFlavor);
            return new URI(text).toURL();
		} catch (UnsupportedFlavorException | IOException | URISyntaxException | IllegalArgumentException e) {
			return null;
		}
	}

	private Optional<Document> parse(URL url, ProgressIndicator progress) {
		progress.setText("Getting html document");
		try {
			Response response = Jsoup.connect(url.toString()).followRedirects(false).timeout(2000)
					.maxBodySize(MAX_BODY_SIZE).execute();
			if (response.statusCode() != 200) {
				return Optional.empty();
			}
			return Optional.of(response.parse());
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	private Optional<String> getFavIconAsBase64(URL url, Document document, ProgressIndicator progress) {
		progress.setText("Getting favIcon");
		Optional<String> favIconUrl = getFavIconUrl(document);
		if (favIconUrl.isEmpty()) {
			favIconUrl = getFavIconUrl(url);
		}
		return favIconUrl.flatMap(u -> getFavIconAsBase64(u, progress));
	}

	private Optional<String> getFavIconUrl(URL url) {
		try {
			URI uri = url.toURI();
			URI favIconUri = new URI(uri.getScheme(), uri.getAuthority(), "/favicon.ico", null, null);
			return Optional.of(favIconUri.toString());
		} catch (URISyntaxException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private Optional<String> getFavIconAsBase64(String favIconUrl, ProgressIndicator progress) {
		progress.setText("Getting favIcon");
		Response resultImageResponse;
		try {
			resultImageResponse = Jsoup.connect(favIconUrl).ignoreContentType(true).execute();
		} catch (IOException e) {
			return Optional.empty();
		}
		byte[] bytes = resultImageResponse.bodyAsBytes();
		Optional<BufferedImage> bufferedImage = getBufferedImage(bytes, TARGET_ICON_SIZE);
		return bufferedImage.map(imgData -> Base64.getEncoder().encodeToString(asBytes(imgData, PNG_FORMAT_NAME)));
	}

	private byte[] asBytes(BufferedImage imageData, String formatName) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(imageData, formatName, baos);
			return baos.toByteArray();
		} catch (IOException e) {
			return new byte[0];
		}
	}

	private List<BufferedImage> getBufferedImages(byte[] favIconBytes) throws IOException {
		try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(favIconBytes))) {
			Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
			if (!iter.hasNext()) {
				return Collections.emptyList();
			}
			ImageReader reader = iter.next();
			try {
				ImageReadParam param = reader.getDefaultReadParam();
				reader.setInput(stream, true, true);
				List<BufferedImage> bufferedImages = new ArrayList<>();
				int i = 0;
				while (true) {
					try {
						BufferedImage bufferedImage = reader.read(i, param);
						bufferedImages.add(bufferedImage);
					} catch (IndexOutOfBoundsException e) {
						return bufferedImages;
					}
					i++;
				}
			} finally {
				reader.dispose();
			}
		}
	}

	private Optional<BufferedImage> getBufferedImage(byte[] favIconBytes, int targetImageSize) {
		List<BufferedImage> bufferedImages;
		try {
			bufferedImages = getBufferedImages(favIconBytes);
		} catch (IOException e) {
			return Optional.empty();
		}
		Optional<BufferedImage> optionalBufferedImage = bufferedImages.stream().min((image1, image2) -> distanceFromTargetImageSize(image1, targetImageSize)
                - distanceFromTargetImageSize(image2, targetImageSize));
		if (optionalBufferedImage.isEmpty()) {
			return Optional.empty();
		}
		BufferedImage bufferedImage = optionalBufferedImage.get();
		if (bufferedImage.getWidth() <= targetImageSize && bufferedImage.getHeight() <= targetImageSize) {
			return Optional.of(bufferedImage);
		}
		return Optional.of(resizeImage(bufferedImage, TARGET_ICON_SIZE, TARGET_ICON_SIZE));
	}

	private int distanceFromTargetImageSize(BufferedImage image, int targetImageSize) {
		return Math.abs(image.getWidth() * image.getHeight() - targetImageSize * targetImageSize);
	}

	private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
		return Scalr.resize(originalImage, Method.ULTRA_QUALITY, width, height);
	}

	private Optional<String> getFavIconUrl(Document document) {
		Element head = document.head();
        Element link = head.select("link[href~=.*\\.(ico|png|gif)]").first();
		if (link == null) {
			return Optional.empty();
		}
		return Optional.of(link.attr("abs:href"));
	}

	private Optional<String> getTitle(Document document) {
		return Optional.of(document.title());
	}

}
