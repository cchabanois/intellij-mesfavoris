package mesfavoris.gdrive.operations;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ParentReference;

import java.io.IOException;

public class AddFileToFolderOperation extends AbstractGDriveOperation {

	public AddFileToFolderOperation(Drive drive) {
		super(drive);
	}

	public void addToFolder(String folderId, String fileId) throws IOException {
		ParentReference newParent = new ParentReference();
		newParent.setId(folderId);
		drive.parents().insert(fileId, newParent).execute();
	}

}
