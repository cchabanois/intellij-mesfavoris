package mesfavoris.gdrive.operations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.intellij.openapi.progress.ProgressIndicator;

import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.services.drive.Drive;

public class DownloadFileOperation extends AbstractGDriveOperation {

	public DownloadFileOperation(Drive drive) {
		super(drive);
	}

	public byte[] downloadFile(String fileId, ProgressIndicator progressIndicator) throws IOException {
		Drive.Files.Get get = drive.files().get(fileId);
		MediaHttpDownloader mediaHttpDownloader = get.getMediaHttpDownloader();
		mediaHttpDownloader.setDirectDownloadEnabled(true);

		FileDownloadProgressListener downloadProgressListener = new FileDownloadProgressListener(
				progressIndicator);
		mediaHttpDownloader.setProgressListener(downloadProgressListener);
		downloadProgressListener.begin();
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			get.executeMediaAndDownloadTo(baos);
			return baos.toByteArray();
		} finally {
			downloadProgressListener.done();
		}
	}

}
