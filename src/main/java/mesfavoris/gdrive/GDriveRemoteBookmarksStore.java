package mesfavoris.gdrive;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.gdrive.changes.BookmarksFileChangeManager;
import mesfavoris.gdrive.connection.GDriveConnectionManager;
import mesfavoris.gdrive.connection.IConnectionListener;
import mesfavoris.gdrive.mappings.*;
import mesfavoris.gdrive.operations.CreateFileOperation;
import mesfavoris.gdrive.operations.DownloadHeadRevisionOperation;
import mesfavoris.gdrive.operations.DownloadHeadRevisionOperation.FileContents;
import mesfavoris.gdrive.operations.TrashFileOperation;
import mesfavoris.gdrive.operations.UpdateFileOperation;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksTreeDeserializer;
import mesfavoris.persistence.IBookmarksTreeSerializer;
import mesfavoris.persistence.json.BookmarksTreeJsonDeserializer;
import mesfavoris.persistence.json.BookmarksTreeJsonSerializer;
import mesfavoris.remote.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static mesfavoris.gdrive.operations.BookmarkFileConstants.MESFAVORIS_MIME_TYPE;

public class GDriveRemoteBookmarksStore extends AbstractRemoteBookmarksStore {
	private final GDriveConnectionManager gDriveConnectionManager;
	private final BookmarkMappingsStore bookmarkMappingsStore;
	private final BookmarksFileChangeManager bookmarksFileChangeManager;
	private final Duration durationForNewRevision;
	private final IBookmarkMappingPropertiesProvider bookmarkMappingPropertiesProvider;

	public GDriveRemoteBookmarksStore(Project project, GDriveConnectionManager gDriveConnectionManager,
			BookmarkMappingsStore bookmarksMappingsStore, BookmarksFileChangeManager bookmarksFileChangeManager) {
		super(project);
		this.gDriveConnectionManager = gDriveConnectionManager;
		this.bookmarkMappingsStore = bookmarksMappingsStore;
		this.bookmarksFileChangeManager = bookmarksFileChangeManager;
		this.durationForNewRevision = Duration.ofMinutes(2);
		this.gDriveConnectionManager.addConnectionListener(new IConnectionListener() {

			@Override
			public void disconnected() {
				postDisconnected();
			}

			@Override
			public void connected() {
				postConnected();
			}
		});
		this.bookmarkMappingsStore.addListener(new IBookmarkMappingsListener() {

			@Override
			public void mappingRemoved(BookmarkId bookmarkFolderId) {
				postMappingRemoved(bookmarkFolderId);
			}

			@Override
			public void mappingAdded(BookmarkId bookmarkFolderId) {
				postMappingAdded(bookmarkFolderId);
			}
		});
		this.bookmarksFileChangeManager.addListener((bookmarkFolderId, change) -> postRemoteBookmarksTreeChanged(bookmarkFolderId));
		this.bookmarkMappingPropertiesProvider = new BookmarkMappingPropertiesProvider();
	}

	@Override
	public void connect(ProgressIndicator indicator) throws IOException {
		gDriveConnectionManager.connect(indicator);
	}

	@Override
	public void disconnect(ProgressIndicator indicator) {
		gDriveConnectionManager.disconnect(indicator);
	}

	@Override
	public State getState() {
		return gDriveConnectionManager.getState();
	}

	@Override
	public RemoteBookmarksTree add(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId, ProgressIndicator indicator)
			throws IOException {
		if (indicator != null) {
			indicator.setText("Saving bookmark folder");
			indicator.setFraction(0.0);
		}
		Drive drive = gDriveConnectionManager.getDrive();
		String bookmarkDirId = gDriveConnectionManager.getApplicationFolderId();
		if (drive == null || bookmarkDirId == null) {
			throw new IllegalStateException("Not connected");
		}
		BookmarkFolder bookmarkFolder = (BookmarkFolder) bookmarksTree.getBookmark(bookmarkFolderId);
		if (bookmarkFolder == null) {
			throw new IllegalArgumentException("Cannot find folder with id " + bookmarkFolderId);
		}
		CreateFileOperation createFileOperation = new CreateFileOperation(drive);
		byte[] content = serializeBookmarkFolder(bookmarksTree, bookmarkFolderId, indicator);
		if (indicator != null) {
			indicator.setFraction(0.2);
		}
		File file = createFileOperation.createFile(bookmarkDirId,
				bookmarkFolder.getPropertyValue(Bookmark.PROPERTY_NAME), MESFAVORIS_MIME_TYPE, content,
				indicator);
		BookmarksTree bookmarkFolderTree = bookmarksTree.subTree(bookmarkFolderId);
		bookmarkMappingsStore.add(bookmarkFolder.getId(), file.getId(),
				bookmarkMappingPropertiesProvider.getBookmarkMappingProperties(file, bookmarkFolderTree));
		return new RemoteBookmarksTree(this, bookmarkFolderTree, file.getEtag());
	}

	private byte[] serializeBookmarkFolder(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId,
			ProgressIndicator indicator) throws IOException {
		IBookmarksTreeSerializer serializer = new BookmarksTreeJsonSerializer(true);
		StringWriter writer = new StringWriter();
		serializer.serialize(bookmarksTree, bookmarkFolderId, writer);
		byte[] content = writer.getBuffer().toString().getBytes(StandardCharsets.UTF_8);
		return content;
	}

	@Override
	public void remove(BookmarkId bookmarkFolderId, ProgressIndicator indicator) throws IOException {
		if (indicator != null) {
			indicator.setText("Removing bookmark folder from gDrive");
			indicator.setFraction(0.0);
		}
		Drive drive = gDriveConnectionManager.getDrive();
		if (drive == null) {
			throw new IllegalStateException("Not connected");
		}
		String fileId = bookmarkMappingsStore.getMapping(bookmarkFolderId).map(BookmarkMapping::getFileId)
				.orElseThrow(() -> new IllegalArgumentException("This folder has not been added to gDrive"));
		bookmarkMappingsStore.remove(bookmarkFolderId);
		if (indicator != null) {
			indicator.setFraction(0.1);
		}
		TrashFileOperation trashFileOperation = new TrashFileOperation(drive);
		trashFileOperation.trashFile(fileId);
		if (indicator != null) {
			indicator.setFraction(1.0);
		}
	}

	@Override
	public Set<RemoteBookmarkFolder> getRemoteBookmarkFolders() {
		return bookmarkMappingsStore.getMappings().stream()
				.map(mapping -> new RemoteBookmarkFolder(getDescriptor().id(), mapping.getBookmarkFolderId(),
						mapping.getProperties()))
				.collect(Collectors.toSet());
	}

	@Override
	public Optional<RemoteBookmarkFolder> getRemoteBookmarkFolder(BookmarkId bookmarkFolderId) {
		return bookmarkMappingsStore.getMapping(bookmarkFolderId)
				.map(mapping -> new RemoteBookmarkFolder(getDescriptor().id(), mapping.getBookmarkFolderId(),
						mapping.getProperties()));
	}

	@Override
	public RemoteBookmarksTree load(BookmarkId bookmarkFolderId, ProgressIndicator indicator) throws IOException {
		if (indicator != null) {
			indicator.setText("Loading bookmark folder");
			indicator.setFraction(0.0);
		}
		Drive drive = gDriveConnectionManager.getDrive();
		String bookmarkDirId = gDriveConnectionManager.getApplicationFolderId();
		if (drive == null || bookmarkDirId == null) {
			throw new IllegalStateException("Not connected");
		}
		String fileId = bookmarkMappingsStore.getMapping(bookmarkFolderId).map(BookmarkMapping::getFileId)
				.orElseThrow(() -> new IllegalArgumentException("This folder has not been added to gDrive"));
		DownloadHeadRevisionOperation downloadFileOperation = new DownloadHeadRevisionOperation(drive);
		FileContents contents = downloadFileOperation.downloadFile(fileId, indicator);
		if (indicator != null) {
			indicator.setFraction(0.8);
		}
		IBookmarksTreeDeserializer deserializer = new BookmarksTreeJsonDeserializer();
		BookmarksTree bookmarkFolderTree = deserializer
				.deserialize(new StringReader(new String(contents.getFileContents(), StandardCharsets.UTF_8)));
		bookmarkMappingsStore.update(contents.getFile().getId(),
				bookmarkMappingPropertiesProvider.getBookmarkMappingProperties(contents.getFile(), bookmarkFolderTree));
		return new RemoteBookmarksTree(this, bookmarkFolderTree, contents.getFile().getEtag());
	}

	@Override
	public RemoteBookmarksTree save(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId, String etag,
			ProgressIndicator indicator) throws IOException, ConflictException {
		if (indicator != null) {
			indicator.setText("Saving bookmark folder");
			indicator.setFraction(0.0);
		}
		Drive drive = gDriveConnectionManager.getDrive();
		String bookmarkDirId = gDriveConnectionManager.getApplicationFolderId();
		if (drive == null || bookmarkDirId == null) {
			throw new IllegalStateException("Not connected");
		}
		String fileId = bookmarkMappingsStore.getMapping(bookmarkFolderId).map(BookmarkMapping::getFileId)
				.orElseThrow(() -> new IllegalArgumentException("This folder has not been added to gDrive"));
		try {
			UpdateFileOperation updateFileOperation = new UpdateFileOperation(drive, durationForNewRevision);
			byte[] content = serializeBookmarkFolder(bookmarksTree, bookmarkFolderId, indicator);
			if (indicator != null) {
				indicator.setFraction(0.2);
			}
			File file = updateFileOperation.updateFile(fileId, MESFAVORIS_MIME_TYPE, content, etag,
					indicator);
			BookmarksTree bookmarkFolderTree = bookmarksTree.subTree(bookmarkFolderId);
			bookmarkMappingsStore.update(file.getId(),
					bookmarkMappingPropertiesProvider.getBookmarkMappingProperties(file, bookmarkFolderTree));
			return new RemoteBookmarksTree(this, bookmarkFolderTree, file.getEtag());
		} catch (GoogleJsonResponseException e) {
			if (e.getStatusCode() == 412) {
				// Precondition Failed
				throw new ConflictException();
			} else {
				throw new IOException(e);
			}
		}
	}

	@Override
	public UserInfo getUserInfo() {
		return gDriveConnectionManager.getUserInfo();
	}

}
