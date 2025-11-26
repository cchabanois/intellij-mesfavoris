package mesfavoris.gdrive.operations;

import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.diagnostic.Logger;

import java.net.URI;
import java.net.URISyntaxException;

public class ViewInGDriveOperation {
	private static final Logger LOG = Logger.getInstance(ViewInGDriveOperation.class);

	public void viewInGDrive(String fileId) {
		String url = "https://drive.google.com/open?id=" + fileId;
		try {
			BrowserLauncher.getInstance().browse(new URI(url));
		} catch (URISyntaxException e) {
			LOG.error("Could not open browser", e);
		}
	}
}
