package mesfavoris.gdrive.operations;

import com.google.api.services.drive.model.File;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.gdrive.GDriveTestUser;
import mesfavoris.gdrive.operations.DownloadHeadRevisionOperation.FileContents;
import mesfavoris.gdrive.test.GDriveConnectionRule;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class ShareFileOperationTest extends BasePlatformTestCase {

	private GDriveConnectionRule gdriveConnectionUser1;
	private GDriveConnectionRule gdriveConnectionUser2;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		gdriveConnectionUser1 = new GDriveConnectionRule(GDriveTestUser.USER1, true);
		gdriveConnectionUser1.before();
		gdriveConnectionUser2 = new GDriveConnectionRule(GDriveTestUser.USER2, true);
		gdriveConnectionUser2.before();
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			if (gdriveConnectionUser2 != null) {
				gdriveConnectionUser2.after();
			}
			if (gdriveConnectionUser1 != null) {
				gdriveConnectionUser1.after();
			}
		} finally {
			super.tearDown();
		}
	}

	public void testShareWithUser() throws Exception {
		// Given
		File file = createTextFile(gdriveConnectionUser1, "myFile.txt", "the contents");
		ShareFileOperation shareFileOperation = new ShareFileOperation(gdriveConnectionUser1.getDrive());

		// When
		shareFileOperation.shareWithUser(file.getId(), GDriveTestUser.USER2.getEmail(), true);

		// Then
		byte[] contentsAsBytes = downloadHeadRevision(gdriveConnectionUser2, file.getId());
		assertThat(new String(contentsAsBytes, StandardCharsets.UTF_8)).isEqualTo("the contents");
	}

	public void testShareWithAnyoneWithLink() throws Exception {
		// Given
		File file = createTextFile(gdriveConnectionUser1, "myFile.txt", "the contents");
		ShareFileOperation shareFileOperation = new ShareFileOperation(gdriveConnectionUser1.getDrive());

		// When
		shareFileOperation.shareWithAnyone(file.getId(), false, true);

		// Then
		byte[] contentsAsBytes = downloadHeadRevision(gdriveConnectionUser2, file.getId());
		assertThat(new String(contentsAsBytes, StandardCharsets.UTF_8)).isEqualTo("the contents");
	}

	private File createTextFile(GDriveConnectionRule driveConnection, String name, String contents)
			throws UnsupportedEncodingException, IOException {
		CreateFileOperation createFileOperation = new CreateFileOperation(driveConnection.getDrive());
		File file = createFileOperation.createFile(driveConnection.getApplicationFolderId(), name, "text/plain",
				contents.getBytes(StandardCharsets.UTF_8), new EmptyProgressIndicator());
		return file;
	}

	private byte[] downloadHeadRevision(GDriveConnectionRule driveConnection, String fileId) throws IOException {
		DownloadHeadRevisionOperation operation = new DownloadHeadRevisionOperation(driveConnection.getDrive());
		FileContents contents = operation.downloadFile(fileId, new EmptyProgressIndicator());
		return contents.getFileContents();
	}

}
