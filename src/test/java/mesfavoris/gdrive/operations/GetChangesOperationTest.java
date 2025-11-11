package mesfavoris.gdrive.operations;

import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.File;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.gdrive.GDriveTestUser;
import mesfavoris.gdrive.test.GDriveConnectionRule;
import mesfavoris.tests.commons.waits.Waiter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class GetChangesOperationTest extends BasePlatformTestCase {

	private GDriveConnectionRule gdriveConnectionRule;
	private GetChangesOperation getChangesOperation;
	private Long startChangeId;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		gdriveConnectionRule = new GDriveConnectionRule(GDriveTestUser.USER1, true);
		gdriveConnectionRule.before();
		getChangesOperation = new GetChangesOperation(gdriveConnectionRule.getDrive());
		startChangeId = getChangesOperation.getLargestChangeId() + 1;
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

	public void testFileAddedChange() throws Exception {
		// Given
		File file1 = createTextFile("file1.txt", "the contents");
		File file2 = createTextFile("file2.txt", "the contents");

		// Then
		waitUntilFileChange(startChangeId, file1.getId());
		waitUntilFileChange(startChangeId, file2.getId());
	}

	public void testGetNextStartChangeId() throws Exception {
		// Given
		File file1 = createTextFile("file1.txt", "the contents");
		waitUntilFileChange(startChangeId, file1.getId());
		List<Change> changes = getChangesOperation.getChanges(startChangeId);

		// When
		startChangeId = changes.get(changes.size() - 1).getId() + 1;
		File file2 = createTextFile("file2.txt", "the contents");

		// Then
		waitUntilFileChange(startChangeId, file2.getId());
	}

	private Change waitUntilFileChange(Long startChangeId, String fileId) throws TimeoutException {
		return Waiter.waitUntil("No change for file " + fileId, () -> getChangesOperation.getChanges(startChangeId)
				.stream().filter(change -> change.getFileId().equals(fileId)).findFirst()).get();
	}

	private File createTextFile(String name, String content) throws IOException {
		CreateFileOperation createFileOperation = new CreateFileOperation(gdriveConnectionRule.getDrive());
		byte[] contents = content.getBytes(StandardCharsets.UTF_8);
		File file = createFileOperation.createFile(gdriveConnectionRule.getApplicationFolderId(), name, "text/plain",
				contents, new EmptyProgressIndicator());
		return file;
	}

}
