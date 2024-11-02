package mesfavoris.persistence;

import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.model.BookmarksTree;

import java.io.IOException;
import java.io.Reader;

public interface IBookmarksTreeDeserializer {

	/**
	 * Deserialize a {@link BookmarksTree}
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public BookmarksTree deserialize(Reader reader) throws IOException;

}