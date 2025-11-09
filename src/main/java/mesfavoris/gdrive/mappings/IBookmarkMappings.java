package mesfavoris.gdrive.mappings;

import mesfavoris.model.BookmarkId;

import java.util.Optional;
import java.util.Set;

public interface IBookmarkMappings {

	Optional<BookmarkMapping> getMapping(BookmarkId bookmarkFolderId);

	Optional<BookmarkMapping> getMapping(String fileId);

	Set<BookmarkMapping> getMappings();
}