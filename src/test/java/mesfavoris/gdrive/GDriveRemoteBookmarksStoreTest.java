package mesfavoris.gdrive;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.concurrency.AppExecutorUtil;
import mesfavoris.gdrive.changes.BookmarksFileChangeManager;
import mesfavoris.gdrive.mappings.BookmarkMappingsStore;
import mesfavoris.gdrive.test.GDriveConnectionRule;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.remote.ConflictException;
import mesfavoris.remote.IRemoteBookmarksStore.State;
import mesfavoris.remote.RemoteBookmarksStoreDescriptor;
import mesfavoris.remote.RemoteBookmarksTree;

import javax.swing.*;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GDriveRemoteBookmarksStoreTest extends BasePlatformTestCase {
	private static final String ID = "gDrive";
	private GDriveRemoteBookmarksStore gDriveRemoteBookmarksStore;
	private BookmarkMappingsStore bookmarkMappingsStore;
	private RemoteBookmarksStoreDescriptor remoteBookmarksStoreDescriptor;
	private BookmarksFileChangeManager bookmarksFileChangeManager;
	private GDriveConnectionRule gDriveConnectionRule;
	private ScheduledExecutorService scheduledExecutorService;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		gDriveConnectionRule = new GDriveConnectionRule(GDriveTestUser.USER1, false);
		gDriveConnectionRule.before();
		scheduledExecutorService = AppExecutorUtil.getAppScheduledExecutorService();
		bookmarkMappingsStore = new BookmarkMappingsStore();
		bookmarksFileChangeManager = new BookmarksFileChangeManager(gDriveConnectionRule.getGDriveConnectionManager(),
				bookmarkMappingsStore, scheduledExecutorService, ()->Duration.ofSeconds(5));
		gDriveRemoteBookmarksStore = new GDriveRemoteBookmarksStore(getProject(),
				gDriveConnectionRule.getGDriveConnectionManager(), bookmarkMappingsStore, bookmarksFileChangeManager);
		remoteBookmarksStoreDescriptor = new RemoteBookmarksStoreDescriptor(ID, "Google Drive",
				new ImageIcon(), new ImageIcon());
		gDriveRemoteBookmarksStore.init(remoteBookmarksStoreDescriptor);
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			if (gDriveRemoteBookmarksStore.getState() == State.connected) {
				disconnect();
			}
			if (gDriveConnectionRule != null) {
				gDriveConnectionRule.after();
			}
		} finally {
			super.tearDown();
		}
	}

	public void testConnect() throws IOException {
		// Given

		connect();

		// Then
		assertThat(gDriveRemoteBookmarksStore.getState()).isEqualTo(State.connected);
	}

	public void testDisconnect() throws IOException {
		// Given
		connect();

		// When
		disconnect();

		// Then
		assertThat(gDriveRemoteBookmarksStore.getState()).isEqualTo(State.disconnected);
	}

	public void testAdd() throws IOException {
		// Given
		BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(new BookmarkId("tree1"), Maps.newHashMap()));
		connect();

		// When
		gDriveRemoteBookmarksStore.add(bookmarksTree, bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());

		// Then
		RemoteBookmarksTree remoteBookmarksTree = gDriveRemoteBookmarksStore.load(bookmarksTree.getRootFolder().getId(),
				new EmptyProgressIndicator());
		assertThat(bookmarksTree.toString()).isEqualTo(remoteBookmarksTree.getBookmarksTree().toString());
		assertThat(gDriveRemoteBookmarksStore.getRemoteBookmarkFolder(bookmarksTree.getRootFolder().getId())).isPresent();
	}

	public void testRemove() throws IOException {
		// Given
		BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(new BookmarkId("tree1"), Maps.newHashMap()));
		connect();
		gDriveRemoteBookmarksStore.add(bookmarksTree, bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());

		// When
		gDriveRemoteBookmarksStore.remove(bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());

		// Then
		assertThat(gDriveRemoteBookmarksStore.getRemoteBookmarkFolders()).isEqualTo(Sets.newHashSet());
	}

	public void testSave() throws Exception {
		// Given
		BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(new BookmarkId("tree1"), Maps.newHashMap()));
		connect();
		RemoteBookmarksTree remoteBookmarksTree = gDriveRemoteBookmarksStore.add(bookmarksTree,
				bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());

		// When
		bookmarksTree = bookmarksTree.setPropertyValue(bookmarksTree.getRootFolder().getId(), "myProperty",
				"myPropertyValue");
		// do not set etag. Otherwise it sometimes fails. Looks like the file is "modified" by gdrive after creation.
		gDriveRemoteBookmarksStore.save(bookmarksTree, bookmarksTree.getRootFolder().getId(), null,
				new EmptyProgressIndicator());

		// Then
		remoteBookmarksTree = gDriveRemoteBookmarksStore.load(bookmarksTree.getRootFolder().getId(),
				new EmptyProgressIndicator());
		assertThat(bookmarksTree.toString()).isEqualTo(remoteBookmarksTree.getBookmarksTree().toString());
	}

	public void testConflictWhenSaving() throws Exception {
		// Given
		BookmarksTree bookmarksTree = new BookmarksTree(new BookmarkFolder(new BookmarkId("tree1"), Maps.newHashMap()));
		connect();
		RemoteBookmarksTree remoteBookmarksTree = gDriveRemoteBookmarksStore.add(bookmarksTree,
				bookmarksTree.getRootFolder().getId(), new EmptyProgressIndicator());
		BookmarksTree bookmarksTree2 = bookmarksTree.setPropertyValue(bookmarksTree.getRootFolder().getId(),
				"myProperty", "myPropertyValue1");
		gDriveRemoteBookmarksStore.save(bookmarksTree2, bookmarksTree2.getRootFolder().getId(),
				remoteBookmarksTree.getEtag(), new EmptyProgressIndicator());

		// When / Then
		BookmarksTree finalBookmarksTree = bookmarksTree.setPropertyValue(bookmarksTree.getRootFolder().getId(), "myProperty",
				"myPropertyValue2");
		assertThatThrownBy(() -> gDriveRemoteBookmarksStore.save(finalBookmarksTree, finalBookmarksTree.getRootFolder().getId(),
				remoteBookmarksTree.getEtag(), new EmptyProgressIndicator()))
				.isInstanceOf(ConflictException.class);
	}

	private void disconnect() {
		gDriveRemoteBookmarksStore.disconnect(new EmptyProgressIndicator());
	}

	private void connect() throws IOException {
		gDriveRemoteBookmarksStore.connect(new EmptyProgressIndicator());
	}

}
