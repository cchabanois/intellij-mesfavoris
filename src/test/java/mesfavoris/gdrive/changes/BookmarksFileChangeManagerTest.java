package mesfavoris.gdrive.changes;

import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.File;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.concurrency.AppExecutorUtil;
import mesfavoris.gdrive.GDriveTestUser;
import mesfavoris.gdrive.mappings.BookmarkMappingsStore;
import mesfavoris.gdrive.operations.CreateFileOperation;
import mesfavoris.gdrive.operations.UpdateFileOperation;
import mesfavoris.gdrive.test.GDriveConnectionRule;
import mesfavoris.model.BookmarkId;
import mesfavoris.tests.commons.waits.Waiter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static mesfavoris.gdrive.operations.BookmarkFileConstants.MESFAVORIS_MIME_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

public class BookmarksFileChangeManagerTest extends BasePlatformTestCase {

	private GDriveConnectionRule gdriveConnectionRule;
	private BookmarksFileChangeManager bookmarksFileChangeManager;
	private BookmarkMappingsStore bookmarkMappings;
	private ScheduledExecutorService scheduledExecutorService;
	private final BookmarksFileChangeListener listener = new BookmarksFileChangeListener();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		gdriveConnectionRule = new GDriveConnectionRule(getProject(), GDriveTestUser.USER1, true);
		gdriveConnectionRule.before();
		scheduledExecutorService = AppExecutorUtil.getAppScheduledExecutorService();
		bookmarkMappings = new BookmarkMappingsStore(getProject());
		bookmarkMappings.add(new BookmarkId("bookmarkFolder1"),
				createFile("bookmarks1", MESFAVORIS_MIME_TYPE, "bookmarks for folder1").getId(), Collections.emptyMap());
		bookmarkMappings.add(new BookmarkId("bookmarkFolder2"),
				createFile("bookmarks2", MESFAVORIS_MIME_TYPE, "bookmarks for folder2").getId(), Collections.emptyMap());
		// wait a few seconds to make sure we don't get events related to previous file creation
		Thread.sleep(10000);
		bookmarksFileChangeManager = new BookmarksFileChangeManager(getProject(), gdriveConnectionRule.getGDriveConnectionManager(),
				bookmarkMappings, scheduledExecutorService, () -> Duration.ofMillis(100));
		bookmarksFileChangeManager.addListener(listener);
		bookmarksFileChangeManager.init();
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			bookmarksFileChangeManager.removeListener(listener);
			bookmarksFileChangeManager.dispose();
			if (gdriveConnectionRule != null) {
				gdriveConnectionRule.after();
			}
		} finally {
			super.tearDown();
		}
	}

	public void testListenerCalledWhenChangeInBookmarksFile() throws Exception {
		// When
		updateFile(bookmarkMappings.getMapping(new BookmarkId("bookmarkFolder1")).get().getFileId(), MESFAVORIS_MIME_TYPE,
				"new bookmarks for folder1");

		// Then
		Waiter.waitUntil("Listener not called", () -> listener.getEvents().size() == 1, Duration.ofSeconds(10));
		Thread.sleep(100);
		assertThat(listener.getEvents()).hasSize(1);
		assertThat(listener.getEvents().get(0).bookmarkFolderId).isEqualTo(new BookmarkId("bookmarkFolder1"));
	}

	public void testListenerNotCalledIfClosed() throws Exception {
		// Given
		bookmarksFileChangeManager.dispose();

		// When
		updateFile(bookmarkMappings.getMapping(new BookmarkId("bookmarkFolder1")).get().getFileId(), MESFAVORIS_MIME_TYPE,
				"new bookmarks for folder1");

		// Then
		Thread.sleep(300);
		assertThat(listener.getEvents()).isEmpty();
	}

	public void testListenerNotCalledIfChangeInAFileThatIsNotABookmarkFile() throws Exception {
		// Given
		File file = createFile("notABookmarkFile", "text/plain", "not bookmarks");

		// When
		updateFile(file.getId(), "text/plain", "really not bookmarks");
		Thread.sleep(300);

		// Then
		assertThat(listener.getEvents()).isEmpty();
	}

	private File createFile(String name, String mimeType, String contents) throws IOException {
		CreateFileOperation createFileOperation = new CreateFileOperation(gdriveConnectionRule.getDrive());
		byte[] bytes = contents.getBytes(UTF_8);
        return createFileOperation.createFile(gdriveConnectionRule.getApplicationFolderId(), name, mimeType, bytes,
                new EmptyProgressIndicator());
	}

	private void updateFile(String fileId, String mimeType, String newContents) throws IOException {
		UpdateFileOperation updateFileOperation = new UpdateFileOperation(gdriveConnectionRule.getDrive());
		updateFileOperation.updateFile(fileId, mimeType, newContents.getBytes(UTF_8), null,
				new EmptyProgressIndicator());
	}

	private static class BookmarksFileChangeListener implements IBookmarksFileChangeListener {
		private final List<BookmarksFileChange> events = new ArrayList<>();

		@Override
		public void bookmarksFileChanged(BookmarkId bookmarkFolderId, Change change) {
			events.add(new BookmarksFileChange(bookmarkFolderId, change));
		}

		public List<BookmarksFileChange> getEvents() {
			return events;
		}

	}

	private static class BookmarksFileChange {
		private final BookmarkId bookmarkFolderId;
		private final Change change;

		public BookmarksFileChange(BookmarkId bookmarkFolderId, Change change) {
			this.bookmarkFolderId = bookmarkFolderId;
			this.change = change;
		}
	}

}
