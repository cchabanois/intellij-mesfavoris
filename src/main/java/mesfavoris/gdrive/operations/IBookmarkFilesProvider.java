package mesfavoris.gdrive.operations;

import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.List;

public interface IBookmarkFilesProvider {

	List<File> getBookmarkFiles() throws IOException;
	
}