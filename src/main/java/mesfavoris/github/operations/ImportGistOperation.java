package mesfavoris.github.operations;

import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.BookmarksException;
import mesfavoris.github.mappings.GistMappingPropertiesProvider;
import mesfavoris.github.mappings.GistMappingsStore;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksTreeDeserializer;
import mesfavoris.persistence.json.BookmarksTreeJsonDeserializer;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public class ImportGistOperation {
    private final GistApiClient apiClient;
    private final GistMappingsStore gistMappingsStore;
    private final IBookmarksService bookmarksService;
    private final GistMappingPropertiesProvider propertiesProvider;

    public ImportGistOperation(GistApiClient apiClient, GistMappingsStore gistMappingsStore,
                               IBookmarksService bookmarksService) {
        this.apiClient = apiClient;
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

        LoadGistOperation.GistContents contents = new LoadGistOperation(apiClient).loadGist(gistId, indicator);

        if (indicator != null) indicator.setFraction(0.6);

        IBookmarksTreeDeserializer deserializer = new BookmarksTreeJsonDeserializer();
        BookmarksTree bookmarksTree = deserializer.deserialize(
                new StringReader(new String(contents.content(), StandardCharsets.UTF_8)));

        if (indicator != null) indicator.setFraction(0.8);

        bookmarksService.addBookmarksTree(parentFolderId, bookmarksTree, tree ->
                gistMappingsStore.add(bookmarksTree.getRootFolder().getId(), gistId,
                        propertiesProvider.getProperties(contents.response(), bookmarksTree)));

        if (indicator != null) indicator.setFraction(1.0);
    }
}
