package mesfavoris.gdrive.operations;

import com.google.api.services.drive.model.File;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.gdrive.GDriveTestUser;
import mesfavoris.gdrive.test.GDriveConnectionRule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CreateFileOperationTest extends BasePlatformTestCase {

	private GDriveConnectionRule gdriveConnectionRule;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		gdriveConnectionRule = new GDriveConnectionRule(getProject(), GDriveTestUser.USER1, true);
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

	public void testCreateFile() throws Exception {
		// Given
		CreateFileOperation createFileOperation = new CreateFileOperation(gdriveConnectionRule.getDrive());
		byte[] contents = "the content".getBytes(StandardCharsets.UTF_8);

		// When
		File file = createFileOperation.createFile(gdriveConnectionRule.getApplicationFolderId(), "myFile.txt",
				"text/plain", contents, new EmptyProgressIndicator());

		// Then
		assertNotNull(file);
		assertEquals("the content", new String(downloadFile(file.getId()), StandardCharsets.UTF_8));
	}

	private byte[] downloadFile(String fileId) throws IOException {
		DownloadFileOperation downloadFileOperation = new DownloadFileOperation(gdriveConnectionRule.getDrive());
		return downloadFileOperation.downloadFile(fileId, new EmptyProgressIndicator());
	}

}
