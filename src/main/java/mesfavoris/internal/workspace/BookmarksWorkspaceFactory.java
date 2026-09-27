package mesfavoris.internal.workspace;

import com.google.common.collect.Lists;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.modification.IBookmarksModificationValidator;
import mesfavoris.persistence.IBookmarksTreeDeserializer;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

public class BookmarksWorkspaceFactory {
	public static final String BOOKMARKS_DATABASE_ID = "main";
	private static final DateTimeFormatter CORRUPTED_SUFFIX_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
	private final IBookmarksTreeDeserializer bookmarksDeserializer;
	private final IBookmarksModificationValidator bookmarksModificationValidator;

	public BookmarksWorkspaceFactory(IBookmarksTreeDeserializer bookmarksDeserializer, IBookmarksModificationValidator bookmarksModificationValidator) {
		this.bookmarksDeserializer = bookmarksDeserializer;
		this.bookmarksModificationValidator = bookmarksModificationValidator;
	}

	public BookmarkDatabase load(File file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
			BookmarksTree bookmarksTree = bookmarksDeserializer.deserialize(reader);
			return new BookmarkDatabase(BOOKMARKS_DATABASE_ID, bookmarksTree, bookmarksModificationValidator);
		}
	}

	/**
	 * Loads the bookmarks file, or creates a new database if there is none. An unreadable file is moved aside (and
	 * reported to {@code onCorruptedFile} with its new path) rather than overwritten by the next save.
	 */
	public BookmarkDatabase loadOrCreate(File file, BiConsumer<Path, IOException> onCorruptedFile) throws IOException {
		try {
			return load(file);
		} catch (NoSuchFileException e) {
			return create();
		} catch (IOException e) {
			Path path = file.toPath();
			Path corruptedFile = path.resolveSibling(
					path.getFileName() + ".corrupted-" + LocalDateTime.now().format(CORRUPTED_SUFFIX_FORMAT));
			try {
				Files.move(path, corruptedFile);
			} catch (IOException moveException) {
				e.addSuppressed(moveException);
				throw e;
			}
			onCorruptedFile.accept(corruptedFile, e);
			return create();
		}
	}

	public BookmarkDatabase create() {
		BookmarkFolder rootFolder = new BookmarkFolder(new BookmarkId("root"), "Root");
		BookmarksTree bookmarksTree = new BookmarksTree(rootFolder);
		BookmarkFolder defaultFolder = new BookmarkFolder(new BookmarkId("default"), "default");
		bookmarksTree = bookmarksTree.addBookmarks(rootFolder.getId(), Lists.newArrayList(defaultFolder));
		return new BookmarkDatabase(BOOKMARKS_DATABASE_ID, bookmarksTree, bookmarksModificationValidator);
	}

}
