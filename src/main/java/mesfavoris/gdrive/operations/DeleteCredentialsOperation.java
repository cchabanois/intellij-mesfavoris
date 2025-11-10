package mesfavoris.gdrive.operations;

import mesfavoris.gdrive.connection.GDriveConnectionManager;
import mesfavoris.gdrive.mappings.IBookmarkMappings;

import java.io.IOException;

public class DeleteCredentialsOperation {
	private final IBookmarkMappings bookmarkMappings;
	private final GDriveConnectionManager gdriveConnectionManager;

	public DeleteCredentialsOperation(GDriveConnectionManager gdriveConnectionManager,
                                      IBookmarkMappings bookmarkMappings) {
		this.gdriveConnectionManager = gdriveConnectionManager;
		this.bookmarkMappings = bookmarkMappings;
	}

	public void deleteCredentials() throws IOException {
		if (!bookmarkMappings.getMappings().isEmpty()) {
			throw new IOException("Cannot delete credentials if there are mappings");
		}
		gdriveConnectionManager.deleteCredentials();
	}

}
