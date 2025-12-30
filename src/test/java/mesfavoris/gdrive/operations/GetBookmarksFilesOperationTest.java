package mesfavoris.gdrive.operations;

import com.google.api.services.drive.model.File;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.gdrive.GDriveTestUser;
import mesfavoris.gdrive.test.GDriveConnectionRule;
import mesfavoris.tests.commons.waits.Waiter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static mesfavoris.gdrive.operations.BookmarkFileConstants.MESFAVORIS_MIME_TYPE;

public class GetBookmarksFilesOperationTest extends BasePlatformTestCase {

	private GDriveConnectionRule gdriveConnectionUser1;
	private GDriveConnectionRule gdriveConnectionUser2;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		gdriveConnectionUser1 = new GDriveConnectionRule(getProject(), GDriveTestUser.USER1, true);
		gdriveConnectionUser1.before();
		gdriveConnectionUser2 = new GDriveConnectionRule(getProject(), GDriveTestUser.USER2, true);
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

	public void testGetBookmarksFile() throws Exception {
		// Given
		File file = createBookmarksFile(gdriveConnectionUser1, "user1File.txt", "the contents");
		GetBookmarkFilesOperation operation = new GetBookmarkFilesOperation(gdriveConnectionUser1.getDrive());

		// Then
		Waiter.waitUntil("Bookmark files does not contain shared file", () -> operation.getBookmarkFiles().stream()
				.map(File::getId).toList().contains(file.getId()), Duration.ofSeconds(20));
	}

	public void testGetSharedBookmarkFile() throws Exception {
		// Given
		File file = createBookmarksFile(gdriveConnectionUser1, "user1File.txt", "the contents");
		share(gdriveConnectionUser1, file.getId(), GDriveTestUser.USER2.getEmail());
		GetBookmarkFilesOperation operation = new GetBookmarkFilesOperation(gdriveConnectionUser2.getDrive());

		// Then
		Waiter.waitUntil("Bookmark files does not contain shared file", () -> operation.getBookmarkFiles().stream()
				.map(File::getId).toList().contains(file.getId()), Duration.ofSeconds(20));
	}

	private File createBookmarksFile(GDriveConnectionRule driveConnection, String name, String contents)
			throws UnsupportedEncodingException, IOException {
		CreateFileOperation createFileOperation = new CreateFileOperation(driveConnection.getDrive());
		File file = createFileOperation.createFile(driveConnection.getApplicationFolderId(), name, MESFAVORIS_MIME_TYPE,
				contents.getBytes(StandardCharsets.UTF_8), new EmptyProgressIndicator());
		return file;
	}

	private void share(GDriveConnectionRule driveConnection, String fileId, String userEmail) throws IOException {
		ShareFileOperation shareFileOperation = new ShareFileOperation(driveConnection.getDrive());
		shareFileOperation.shareWithUser(fileId, userEmail, true);
	}

}
