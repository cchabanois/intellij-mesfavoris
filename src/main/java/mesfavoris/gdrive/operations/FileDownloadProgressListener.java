package mesfavoris.gdrive.operations;

import java.io.IOException;

import com.intellij.openapi.progress.ProgressIndicator;

import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;

/**
 * Track progress when downloading a file
 *
 * @author cchabanois
 *
 */
public class FileDownloadProgressListener implements
		MediaHttpDownloaderProgressListener {
	private final ProgressIndicator progressIndicator;

	public FileDownloadProgressListener(ProgressIndicator progressIndicator) {
		this.progressIndicator = progressIndicator;
	}

	public void begin() {
		progressIndicator.setText("Downloading file");
		progressIndicator.setIndeterminate(false);
		progressIndicator.setFraction(0.0);
	}

	public void done() {
		progressIndicator.setFraction(1.0);
	}

	@Override
	public void progressChanged(MediaHttpDownloader downloader)
			throws IOException {
		switch (downloader.getDownloadState()) {
		case MEDIA_COMPLETE:
			progressIndicator.setText2("Download completed");
			progressIndicator.setFraction(1.0);
			break;
		case MEDIA_IN_PROGRESS:
			progressIndicator.setText2("Download in progress");
			progressIndicator.setFraction(downloader.getProgress());
			break;
		case NOT_STARTED:
			break;
		default:
			break;

		}

	}

}