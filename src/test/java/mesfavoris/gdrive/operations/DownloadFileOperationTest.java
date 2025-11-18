package mesfavoris.gdrive.operations;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Revision;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.gdrive.GDriveTestUser;
import mesfavoris.gdrive.test.GDriveConnectionRule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DownloadFileOperationTest extends BasePlatformTestCase {

	private GDriveConnectionRule gdriveConnectionRule;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		gdriveConnectionRule = new GDriveConnectionRule(GDriveTestUser.USER1, true);
		gdriveConnectionRule.before();
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			if (gdriveConnectionRule != null) {
				gdriveConnectionRule.after();
			}
		} finally {
			super.tearDown();
		}
	}

	public void testDownloadFile() throws Exception {
		// Given
		File file = createTextFile("myFile.txt", "the contents");
		ProgressIndicator progressIndicator = mock(ProgressIndicator.class);
		DownloadFileOperation downloadFileOperation = new DownloadFileOperation(gdriveConnectionRule.getDrive());

		// When
		byte[] contents = downloadFileOperation.downloadFile(file.getId(), progressIndicator);

		// Then
		assertThat(new String(contents, StandardCharsets.UTF_8)).isEqualTo("the contents");
		verify(progressIndicator, atLeast(1)).setText(anyString());
		verify(progressIndicator, atLeast(1)).setFraction(anyDouble());
	}

	public void testETagIsNotTheSameWhenExecuteMedia() throws Exception {
		// Given
		File file = createTextFile("myFile.txt", "the contents");

		// When
		HttpResponse responseExecute = gdriveConnectionRule.getDrive().files().get(file.getId()).executeUnparsed();
		// when using the preferred method to download a file, URL parameter
		// "alt" is set to "media"
		HttpResponse responseExecuteMedia = gdriveConnectionRule.getDrive().files().get(file.getId()).executeMedia();

		// Then
		// unfortunately ...
		assertThat(responseExecute.getHeaders().getETag()).isNotEqualTo(responseExecuteMedia.getHeaders().getETag());

	}

	public void testExecuteMediaETagIsNotRevisionETag() throws Exception {
		// Given
		File file = createTextFile("myFile.txt", "the contents");
		Revision revision = gdriveConnectionRule.getDrive().revisions().get(file.getId(), "head").execute();

		// When
		HttpResponse responseExecuteMedia = gdriveConnectionRule.getDrive().files().get(file.getId()).executeMedia();

		// Then
		assertThat(revision.getEtag()).isNotEqualTo(responseExecuteMedia.getHeaders().getETag());
	}

	public void testDownloadUsingFileDownloadUrl() throws Exception {
		// Given
		File file = createTextFile("myFile.txt", "the contents");
		updateTextFile(file.getId(), "the new contents");

		// When
		byte[] contentsAsBytes = download(file.getDownloadUrl());

		// Then
		// this downloaded the latest version, not the one we expected ...
		assertThat(new String(contentsAsBytes, StandardCharsets.UTF_8)).isEqualTo("the new contents");
	}

	public void testDownloadUsingRevisionDownloadUrl() throws Exception {
		// Given
		File file = createTextFile("myFile.txt", "the contents");
		Revision revision = gdriveConnectionRule.getDrive().revisions().get(file.getId(), "head").execute();
		updateTextFile(file.getId(), "the new contents");

		// When
		byte[] contentsAsBytes = download(revision.getDownloadUrl());

		// Then
		assertThat(new String(contentsAsBytes, StandardCharsets.UTF_8)).isEqualTo("the contents");
		// etag not the same for revision and file ...
		assertThat(file.getEtag()).isNotEqualTo(revision.getEtag());
	}

	private File createTextFile(String name, String contents) throws UnsupportedEncodingException, IOException {
		CreateFileOperation createFileOperation = new CreateFileOperation(gdriveConnectionRule.getDrive());
		File file = createFileOperation.createFile(gdriveConnectionRule.getApplicationFolderId(), name, "text/plain",
				contents.getBytes(StandardCharsets.UTF_8), new EmptyProgressIndicator());
		return file;
	}

	private File getFileMetadata(String fileId) throws IOException {
		GetFileMetadataOperation getFileMetadataOperation = new GetFileMetadataOperation(
				gdriveConnectionRule.getDrive());
		return getFileMetadataOperation.getFileMetadata(fileId);
	}

	private File updateTextFile(String fileId, String newContents) throws UnsupportedEncodingException, IOException {
		UpdateFileOperation updateFileOperation = new UpdateFileOperation(gdriveConnectionRule.getDrive());
		return updateFileOperation.updateFile(fileId, "text/plain", newContents.getBytes(StandardCharsets.UTF_8), null,
				new EmptyProgressIndicator());
	}

	private byte[] download(String downloadUrl) throws IOException {
		HttpRequest get = gdriveConnectionRule.getDrive().getRequestFactory()
				.buildGetRequest(new GenericUrl(downloadUrl));
		HttpResponse response = get.execute();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		response.download(baos);
		return baos.toByteArray();
	}

}
