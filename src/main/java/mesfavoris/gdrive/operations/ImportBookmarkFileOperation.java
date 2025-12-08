package mesfavoris.gdrive.operations;

import com.google.api.services.drive.Drive;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.BookmarksException;
import mesfavoris.gdrive.mappings.BookmarkMappingPropertiesProvider;
import mesfavoris.gdrive.mappings.BookmarkMappingsStore;
import mesfavoris.gdrive.mappings.IBookmarkMappingPropertiesProvider;
import mesfavoris.gdrive.operations.DownloadHeadRevisionOperation.FileContents;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksTreeDeserializer;
import mesfavoris.persistence.json.BookmarksTreeJsonDeserializer;
import mesfavoris.service.IBookmarksService;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ImportBookmarkFileOperation extends AbstractGDriveOperation {
	private static final Logger LOG = Logger.getInstance(ImportBookmarkFileOperation.class);

	private final BookmarkMappingsStore bookmarkMappingsStore;
	private final IBookmarksService bookmarksService;
	private final Optional<String> applicationFolderId;
	private final IBookmarkMappingPropertiesProvider bookmarkMappingPropertiesProvider;

	public ImportBookmarkFileOperation(Drive drive, BookmarkMappingsStore bookmarkMappingsStore,
			IBookmarksService bookmarksService, Optional<String> applicationFolderId) {
		super(drive);
		this.bookmarkMappingsStore = bookmarkMappingsStore;
		this.bookmarksService = bookmarksService;
		this.applicationFolderId = applicationFolderId;
		this.bookmarkMappingPropertiesProvider = new BookmarkMappingPropertiesProvider();
	}

	public void importBookmarkFile(BookmarkId parentId, String fileId, ProgressIndicator progressIndicator)
			throws BookmarksException, IOException {
		progressIndicator.setText("Importing bookmark folder");
		progressIndicator.setIndeterminate(false);
		progressIndicator.setFraction(0.0);

		if (applicationFolderId.isPresent()) {
			// add it to the application folder
			try {
				AddFileToFolderOperation addFileToFolderOperation = new AddFileToFolderOperation(drive);
				addFileToFolderOperation.addToFolder(applicationFolderId.get(), fileId);
			} catch (IOException e) {
				// Cannot be moved if file has been shared with read-only role
				LOG.warn("Could not add file to application folder (file may be read-only): " + fileId, e);
			}
		}

		progressIndicator.setFraction(0.1);
		DownloadHeadRevisionOperation downloadFileOperation = new DownloadHeadRevisionOperation(drive);
		FileContents contents = downloadFileOperation.downloadFile(fileId, progressIndicator);

		progressIndicator.setFraction(0.8);
		IBookmarksTreeDeserializer deserializer = new BookmarksTreeJsonDeserializer();
		BookmarksTree bookmarksTree = deserializer
				.deserialize(new StringReader(new String(contents.getFileContents(), StandardCharsets.UTF_8)));

		bookmarksService.addBookmarksTree(parentId, bookmarksTree, newBookmarksTree -> bookmarkMappingsStore.add(
				bookmarksTree.getRootFolder().getId(), contents.getFile().getId(),
				bookmarkMappingPropertiesProvider.getBookmarkMappingProperties(contents.getFile(), bookmarksTree)));

		progressIndicator.setFraction(1.0);
	}

}
