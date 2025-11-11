package mesfavoris.gdrive.operations;

import java.io.IOException;

import com.intellij.openapi.progress.ProgressIndicator;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;

/**
 * Track progress when uploading a file.
 *
 * @author cchabanois
 *
 */
public class FileUploadProgressListener implements MediaHttpUploaderProgressListener {
	private final ProgressIndicator progressIndicator;

	public FileUploadProgressListener(ProgressIndicator progressIndicator) {
		this.progressIndicator = progressIndicator;
	}

	public void begin() {
		progressIndicator.setText("Uploading file");
		progressIndicator.setIndeterminate(false);
		progressIndicator.setFraction(0.0);
	}

	public void done() {
		progressIndicator.setFraction(1.0);
	}

	@Override
	public void progressChanged(MediaHttpUploader uploader) throws IOException {
		switch (uploader.getUploadState()) {
		case NOT_STARTED:
			break;
		case INITIATION_STARTED:
			progressIndicator.setText2("Upload Initiation has started");
			progressIndicator.setFraction(0.0);
			break;
		case INITIATION_COMPLETE:
			progressIndicator.setText2("Upload Initiation is complete");
			break;
		case MEDIA_IN_PROGRESS:
			progressIndicator.setText2("Upload in progress");
			progressIndicator.setFraction(uploader.getProgress());
			break;
		case MEDIA_COMPLETE:
			progressIndicator.setText2("Upload completed");
			progressIndicator.setFraction(1.0);
			break;

		default:
			break;
		}
	}
}