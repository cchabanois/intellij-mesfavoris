package mesfavoris.gdrive.operations;

import com.google.api.client.util.Lists;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class GetBookmarkFilesOperation extends AbstractGDriveOperation implements IBookmarkFilesProvider {
	private Optional<String> folderId;
	
	public GetBookmarkFilesOperation(Drive drive) {
		this(drive, Optional.empty());
	}
	
	public GetBookmarkFilesOperation(Drive drive, Optional<String> folderId) {
		super(drive);
		this.folderId = folderId;
	}

	@Override
	public List<File> getBookmarkFiles() throws IOException {
		List<File> files = Lists.newArrayList();
		String pageToken = null;
		do {
			StringBuilder query = new StringBuilder("mimeType='"+BookmarkFileConstants.MESFAVORIS_MIME_TYPE+"' and trashed=false");
            folderId.ifPresent(s -> query.append(String.format(" and '%s' in parents", s)));
		    FileList result = drive.files().list()
		            .setQ(query.toString())
		            .setSpaces("drive")
		            .setPageToken(pageToken)
		            .execute();
		    files.addAll(result.getItems());
		    pageToken = result.getNextPageToken();
		} while (pageToken != null);
		return files;
	}
	
}
