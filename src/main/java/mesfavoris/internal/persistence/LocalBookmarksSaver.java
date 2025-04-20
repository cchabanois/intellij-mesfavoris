package mesfavoris.internal.persistence;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksTreeSerializer;

public class LocalBookmarksSaver {
	private final File file;
	private final IBookmarksTreeSerializer bookmarksSerializer;
	
	public LocalBookmarksSaver(File file, IBookmarksTreeSerializer bookmarksSerializer) {
		this.file = file;
		this.bookmarksSerializer = bookmarksSerializer;
	}
	
	public void saveBookmarks(BookmarksTree bookmarksTree) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(file);
			bookmarksSerializer.serialize(bookmarksTree,
					bookmarksTree.getRootFolder().getId(), writer);
		} catch (IOException e) {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException ignored) {
				}
			}
		}
	}	
	
}
