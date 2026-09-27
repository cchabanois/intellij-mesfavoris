package mesfavoris.internal.workspace;

import mesfavoris.internal.validation.AcceptAllBookmarksModificationValidator;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.persistence.json.BookmarksTreeJsonDeserializer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BookmarksWorkspaceFactoryTest {
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private BookmarksWorkspaceFactory factory;
	private File bookmarksFile;
	private final List<Path> corruptedFiles = new ArrayList<>();

	@Before
	public void setUp() throws IOException {
		factory = new BookmarksWorkspaceFactory(new BookmarksTreeJsonDeserializer(),
				new AcceptAllBookmarksModificationValidator());
		bookmarksFile = new File(temporaryFolder.getRoot(), "bookmarks.json");
	}

	@Test
	public void testLoadOrCreateLoadsExistingFile() throws IOException {
		// Given
		Files.writeString(bookmarksFile.toPath(), """
				{"version":"1.0","bookmarks":{"id":"root","children":[
				  {"id":"b1","properties":{"name":"été"}}]}}""", StandardCharsets.UTF_8);

		// When
		BookmarkDatabase database = factory.loadOrCreate(bookmarksFile, (path, e) -> corruptedFiles.add(path));

		// Then
		assertThat(database.getBookmarksTree().getBookmark(new BookmarkId("b1")).getPropertyValue("name"))
				.isEqualTo("été");
		assertThat(corruptedFiles).isEmpty();
	}

	@Test
	public void testLoadOrCreateCreatesDatabaseIfNoFile() throws IOException {
		// When
		BookmarkDatabase database = factory.loadOrCreate(bookmarksFile, (path, e) -> corruptedFiles.add(path));

		// Then
		assertThat(database.getBookmarksTree().getBookmark(new BookmarkId("default"))).isNotNull();
		assertThat(corruptedFiles).isEmpty();
	}

	@Test
	public void testLoadOrCreateMovesCorruptedFileAside() throws IOException {
		// Given
		String truncatedContent = "{\"version\":\"1.0\",\"bookmarks\":{\"id\":\"root\",\"chil";
		Files.writeString(bookmarksFile.toPath(), truncatedContent);

		// When
		BookmarkDatabase database = factory.loadOrCreate(bookmarksFile, (path, e) -> corruptedFiles.add(path));

		// Then
		assertThat(database.getBookmarksTree().getBookmark(new BookmarkId("default"))).isNotNull();
		assertThat(bookmarksFile).doesNotExist();
		assertThat(corruptedFiles).hasSize(1);
		assertThat(corruptedFiles.getFirst().getFileName().toString()).startsWith("bookmarks.json.corrupted-");
		assertThat(corruptedFiles.getFirst()).hasContent(truncatedContent);
	}

}
