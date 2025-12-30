package mesfavoris.gdrive.operations;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Revisions.List;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.RevisionList;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.gdrive.GDriveTestUser;
import mesfavoris.gdrive.test.GDriveConnectionRule;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdateFileOperationTest extends BasePlatformTestCase {

	private static final String TEXT_MIMETYPE = "text/plain";

	private GDriveConnectionRule gdriveConnectionUser1;
	private GDriveConnectionRule gdriveConnectionUser2;
	private Clock clock;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		gdriveConnectionUser1 = new GDriveConnectionRule(getProject(), GDriveTestUser.USER1, true);
		gdriveConnectionUser1.before();
		gdriveConnectionUser2 = new GDriveConnectionRule(getProject(), GDriveTestUser.USER2, true);
		gdriveConnectionUser2.before();
		clock = mock(Clock.class);
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

	public void testUpdateFile() throws Exception {
		// Given
		File file = createTextFile(gdriveConnectionUser1, "myFile.txt", "the contents");
		UpdateFileOperation updateFileOperation = new UpdateFileOperation(gdriveConnectionUser1.getDrive(), clock,
				Duration.ofMinutes(10));

		// When
		// do not set etag. Otherwise it sometimes fails. Looks like the file is
		// "modified" by gdrive after creation.
		when(clock.instant()).thenReturn(lastModified(gdriveConnectionUser1, file.getId()).plus(4, ChronoUnit.MINUTES));
		updateFileOperation.updateFile(file.getId(), TEXT_MIMETYPE, "the new contents".getBytes(UTF_8), null,
				new EmptyProgressIndicator());

		// Then
		assertThat(new String(downloadFile(gdriveConnectionUser1, file.getId()), UTF_8)).isEqualTo("the new contents");
	}

	public void testUpdateFileDoesNotCreateNewRevisionIfModificationsAreCloseInTime() throws Exception {
		// Given
		File file = createTextFile(gdriveConnectionUser1, "myFile.txt", "original contents");
		UpdateFileOperation updateFileOperation = new UpdateFileOperation(gdriveConnectionUser1.getDrive(), clock,
				Duration.ofMinutes(10));

		// When
		// do not set etag. Otherwise it sometimes fails. Looks like the file is
		// "modified" by gdrive after creation.
		when(clock.instant()).thenReturn(lastModified(gdriveConnectionUser1, file.getId()).plus(4, ChronoUnit.MINUTES));
		updateFileOperation.updateFile(file.getId(), TEXT_MIMETYPE, "first modification".getBytes(UTF_8), null,
				new EmptyProgressIndicator());
		when(clock.instant()).thenReturn(lastModified(gdriveConnectionUser1, file.getId()).plus(4, ChronoUnit.MINUTES));
		updateFileOperation.updateFile(file.getId(), TEXT_MIMETYPE, "second modification".getBytes(UTF_8),
				null, new EmptyProgressIndicator());

		// Then
		assertThat(new String(downloadFile(gdriveConnectionUser1, file.getId()), UTF_8)).isEqualTo("second modification");
		assertThat(getRevisionsCount(gdriveConnectionUser1, file.getId())).isEqualTo(1);
	}

	public void testUpdateFileCreatesNewRevisionIfUserIsNotTheSameThanForPreviousModification() throws Exception {
		// Given
		File file = createTextFile(gdriveConnectionUser1, "myFile.txt", "original contents");
		share(gdriveConnectionUser1, file.getId(), GDriveTestUser.USER2.getEmail());
		UpdateFileOperation updateFileOperationUser1 = new UpdateFileOperation(gdriveConnectionUser1.getDrive(), clock,
				Duration.ofMinutes(10));
		UpdateFileOperation updateFileOperationUser2 = new UpdateFileOperation(gdriveConnectionUser2.getDrive(), clock,
				Duration.ofMinutes(10));

		// When
		// do not set etag. Otherwise it sometimes fails. Looks like the file is
		// "modified" by gdrive after creation.
		when(clock.instant()).thenReturn(lastModified(gdriveConnectionUser1, file.getId()).plus(4, ChronoUnit.MINUTES));
		when(clock.instant()).thenReturn(lastModified(gdriveConnectionUser2, file.getId()).plus(4, ChronoUnit.MINUTES));
		updateFileOperationUser1.updateFile(file.getId(), TEXT_MIMETYPE, "first modification".getBytes(UTF_8),
				null, new EmptyProgressIndicator());
		updateFileOperationUser2.updateFile(file.getId(), TEXT_MIMETYPE, "second modification".getBytes(UTF_8),
				null, new EmptyProgressIndicator());

		// Then
		assertThat(new String(downloadFile(gdriveConnectionUser1, file.getId()), UTF_8)).isEqualTo("second modification");
		assertThat(getRevisionsCount(gdriveConnectionUser1, file.getId())).isEqualTo(2);
	}

	public void testUpdateFileCreatesNewRevisionIfModificationsAreNotCloseInTime() throws Exception {
		// Given
		File file = createTextFile(gdriveConnectionUser1, "myFile.txt", "original contents");
		UpdateFileOperation updateFileOperation = new UpdateFileOperation(gdriveConnectionUser1.getDrive(), clock,
				Duration.ofMinutes(10));

		// When
		// do not set etag. Otherwise it sometimes fails. Looks like the file is
		// "modified" by gdrive after creation.
		when(clock.instant()).thenReturn(lastModified(gdriveConnectionUser1, file.getId()).plus(4, ChronoUnit.MINUTES));
		updateFileOperation.updateFile(file.getId(), TEXT_MIMETYPE, "first modification".getBytes(UTF_8), null,
				new EmptyProgressIndicator());
		when(clock.instant())
				.thenReturn(lastModified(gdriveConnectionUser1, file.getId()).plus(11, ChronoUnit.MINUTES));
		updateFileOperation.updateFile(file.getId(), TEXT_MIMETYPE, "second modification".getBytes(UTF_8),
				null, new EmptyProgressIndicator());

		// Then
		assertThat(new String(downloadFile(gdriveConnectionUser1, file.getId()), UTF_8)).isEqualTo("second modification");
		assertThat(getRevisionsCount(gdriveConnectionUser1, file.getId())).isEqualTo(2);
	}

	public void testCannotUpdateFileThatHasBeenUpdatedSince() throws Exception {
		// Given
		File file = createTextFile(gdriveConnectionUser1, "myFile.txt", "the contents");
		UpdateFileOperation updateFileOperation = new UpdateFileOperation(gdriveConnectionUser1.getDrive(), clock,
				Duration.ofMinutes(10));
		when(clock.instant()).thenReturn(lastModified(gdriveConnectionUser1, file.getId()).plus(4, ChronoUnit.MINUTES));
		updateFileOperation.updateFile(file.getId(), TEXT_MIMETYPE, "the new contents".getBytes(UTF_8),
				file.getEtag(), new EmptyProgressIndicator());

		// When / Then
		when(clock.instant()).thenReturn(lastModified(gdriveConnectionUser1, file.getId()).plus(4, ChronoUnit.MINUTES));
		assertThatThrownBy(() -> updateFileOperation.updateFile(file.getId(), TEXT_MIMETYPE, "the newest contents".getBytes(UTF_8),
                file.getEtag(), new EmptyProgressIndicator())).isInstanceOf(GoogleJsonResponseException.class)
		  .hasMessageContaining("412 Precondition Failed");
	}

	private File createTextFile(GDriveConnectionRule connection, String name, String contents)
			throws UnsupportedEncodingException, IOException {
		CreateFileOperation createFileOperation = new CreateFileOperation(connection.getDrive());
		File file = createFileOperation.createFile(connection.getApplicationFolderId(), name, TEXT_MIMETYPE,
				contents.getBytes(UTF_8), new EmptyProgressIndicator());
		return file;
	}

	private byte[] downloadFile(GDriveConnectionRule connection, String fileId) throws IOException {
		DownloadFileOperation downloadFileOperation = new DownloadFileOperation(connection.getDrive());
		return downloadFileOperation.downloadFile(fileId, new EmptyProgressIndicator());
	}

	private int getRevisionsCount(GDriveConnectionRule connection, String fileId) throws IOException {
		List list = connection.getDrive().revisions().list(fileId);
		RevisionList revisionList = list.execute();
		return revisionList.getItems().size();
	}

	private void share(GDriveConnectionRule driveConnection, String fileId, String userEmail) throws IOException {
		ShareFileOperation shareFileOperation = new ShareFileOperation(driveConnection.getDrive());
		shareFileOperation.shareWithUser(fileId, userEmail, true);
	}

	private Instant lastModified(GDriveConnectionRule connection, String fileId) throws IOException {
		Drive.Files.Get get = connection.getDrive().files().get(fileId);
		File latestFileVersion = get.execute();
        return Instant.ofEpochMilli(latestFileVersion.getModifiedDate().getValue());
	}

}
