package mesfavoris.gdrive.operations;

import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;

import com.intellij.openapi.progress.ProgressIndicator;

import com.google.api.services.drive.Drive;

import mesfavoris.BookmarksException;
import mesfavoris.gdrive.mappings.BookmarkMappingPropertiesProvider;
import mesfavoris.gdrive.mappings.BookmarkMappingsStore;
import mesfavoris.gdrive.mappings.IBookmarkMappingPropertiesProvider;
import mesfavoris.gdrive.operations.DownloadHeadRevisionOperation.FileContents;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksTreeDeserializer;
import mesfavoris.persistence.json.BookmarksTreeJsonDeserializer;
import mesfavoris.service.BookmarksService;

public class ImportBookmarkFileOperation extends AbstractGDriveOperation {
	private final BookmarkMappingsStore bookmarkMappingsStore;
	private final BookmarksService bookmarksService;
	private final Optional<String> applicationFolderId;
	private final IBookmarkMappingPropertiesProvider bookmarkMappingPropertiesProvider;

	public ImportBookmarkFileOperation(Drive drive, BookmarkMappingsStore bookmarkMappingsStore,
			BookmarksService bookmarksService, Optional<String> applicationFolderId) {
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
			AddFileToFolderOperation addFileToFolderOperation = new AddFileToFolderOperation(drive);
			addFileToFolderOperation.addToFolder(applicationFolderId.get(), fileId);
		}

		progressIndicator.setFraction(0.1);
		DownloadHeadRevisionOperation downloadFileOperation = new DownloadHeadRevisionOperation(drive);
		FileContents contents = downloadFileOperation.downloadFile(fileId, progressIndicator);

		progressIndicator.setFraction(0.8);
		IBookmarksTreeDeserializer deserializer = new BookmarksTreeJsonDeserializer();
		BookmarksTree bookmarksTree = deserializer
				.deserialize(new StringReader(new String(contents.getFileContents(), "UTF-8")));

		bookmarksService.addBookmarksTree(parentId, bookmarksTree, newBookmarksTree -> bookmarkMappingsStore.add(
				bookmarksTree.getRootFolder().getId(), contents.getFile().getId(),
				bookmarkMappingPropertiesProvider.getBookmarkMappingProperties(contents.getFile(), bookmarksTree)));

		progressIndicator.setFraction(1.0);
	}

}
