package mesfavoris.gdrive.operations;

import com.google.api.services.drive.model.File;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.gdrive.GDriveTestUser;
import mesfavoris.gdrive.operations.DownloadHeadRevisionOperation.FileContents;
import mesfavoris.gdrive.test.GDriveConnectionRule;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DownloadHeadRevisionOperationTest extends BasePlatformTestCase {

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

	public void testDownloadFile() throws Exception {
		// Given
		File file = createTextFile("myFile.txt", "the contents");
		ProgressIndicator progressIndicator = mock(ProgressIndicator.class);
		DownloadHeadRevisionOperation downloadFileOperation = new DownloadHeadRevisionOperation(
				gdriveConnectionRule.getDrive());

		// When
		FileContents contents = downloadFileOperation.downloadFile(file.getId(), progressIndicator);

		// Then
		assertThat(new String(contents.getFileContents(), StandardCharsets.UTF_8)).isEqualTo("the contents");
		// don't assert etag because sometimes it changes just after creation
		// assertThat(contents.getFile().getEtag()).isEqualTo(file.getEtag());
		verify(progressIndicator, atLeast(1)).setText(anyString());
		verify(progressIndicator, atLeast(1)).setFraction(anyDouble());
	}

	private File createTextFile(String name, String contents) throws UnsupportedEncodingException, IOException {
		CreateFileOperation createFileOperation = new CreateFileOperation(gdriveConnectionRule.getDrive());
		File file = createFileOperation.createFile(gdriveConnectionRule.getApplicationFolderId(), name, "text/plain",
				contents.getBytes(StandardCharsets.UTF_8), new EmptyProgressIndicator());
		return file;
	}

}
