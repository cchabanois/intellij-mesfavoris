package mesfavoris.github.operations;

import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.BookmarksException;
import mesfavoris.github.client.GistResponse;
import mesfavoris.github.client.IGistApiClient;
import mesfavoris.github.mappings.GistMapping;
import mesfavoris.github.mappings.GistMappingPropertiesProvider;
import mesfavoris.github.mappings.GistMappingsStore;
import mesfavoris.github.repository.GistRepository;
import mesfavoris.github.repository.IGistRepositoryProvider;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksTreeDeserializer;
import mesfavoris.persistence.json.BookmarksTreeJsonDeserializer;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

/** Imports a gist as a bookmark folder: metadata from the REST API, content from the gist's local clone. */
public class ImportGistOperation {
    private final IGistApiClient apiClient;
    private final IGistRepositoryProvider repositoryProvider;
    private final GistMappingsStore gistMappingsStore;
    private final IBookmarksService bookmarksService;
    private final GistMappingPropertiesProvider propertiesProvider;

    public ImportGistOperation(IGistApiClient apiClient, IGistRepositoryProvider repositoryProvider,
                               GistMappingsStore gistMappingsStore, IBookmarksService bookmarksService) {
        this.apiClient = apiClient;
        this.repositoryProvider = repositoryProvider;
        this.gistMappingsStore = gistMappingsStore;
        this.bookmarksService = bookmarksService;
        this.propertiesProvider = new GistMappingPropertiesProvider();
    }

    public void importGist(BookmarkId parentFolderId, String gistId,
                           @Nullable ProgressIndicator indicator) throws IOException, BookmarksException {
        if (indicator != null) {
            indicator.setText("Importing gist " + gistId);
            indicator.setIndeterminate(false);
            indicator.setFraction(0.0);
        }

        GistResponse gist = apiClient.loadGist(gistId);
        if (gist.git_pull_url == null) {
            throw new IOException("No git url for gist " + gistId);
        }

        if (indicator != null) indicator.setFraction(0.3);

        GistRepository.Snapshot snapshot =
                repositoryProvider.getGistRepository(gistId, gist.git_pull_url).pull(GistMapping.BOOKMARKS_FILE_NAME);

        if (indicator != null) indicator.setFraction(0.6);

        IBookmarksTreeDeserializer deserializer = new BookmarksTreeJsonDeserializer();
        BookmarksTree bookmarksTree = deserializer.deserialize(
                new StringReader(new String(snapshot.content(), StandardCharsets.UTF_8)));

        if (indicator != null) indicator.setFraction(0.8);

        bookmarksService.importRemoteBookmarksTree(parentFolderId, bookmarksTree, tree ->
                gistMappingsStore.add(bookmarksTree.getRootFolder().getId(), gistId,
                        propertiesProvider.getProperties(gist, bookmarksTree)));

        if (indicator != null) indicator.setFraction(1.0);
    }
}
