package mesfavoris.github.mappings;

import mesfavoris.model.BookmarkId;

import java.util.Optional;
import java.util.Set;

/** Read-only view of the BookmarkFolder → Gist ID mappings. */
public interface IGistMappings {
    Optional<GistMapping> getMapping(BookmarkId bookmarkFolderId);

    Optional<GistMapping> getMapping(String gistId);

    Set<GistMapping> getMappings();
}
