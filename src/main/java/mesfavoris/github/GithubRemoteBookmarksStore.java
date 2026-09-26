package mesfavoris.github;
import mesfavoris.github.client.IGistApiClient;
import mesfavoris.github.client.GistResponse;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBusConnection;
import mesfavoris.github.changes.GistChangeManager;
import mesfavoris.github.changes.IGistChangeListener;
import mesfavoris.github.connection.GithubConnectionListener;
import mesfavoris.github.connection.GithubConnectionManager;
import mesfavoris.github.mappings.GistMapping;
import mesfavoris.github.mappings.GistMappingPropertiesProvider;
import mesfavoris.github.mappings.GistMappingsStore;
import mesfavoris.github.mappings.IGistMappingsListener;
import mesfavoris.github.operations.*;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksTreeDeserializer;
import mesfavoris.persistence.IBookmarksTreeSerializer;
import mesfavoris.persistence.json.BookmarksTreeJsonDeserializer;
import mesfavoris.persistence.json.BookmarksTreeJsonSerializer;
import mesfavoris.remote.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Synchronizes bookmark folders with GitHub Gists: one private Gist per folder, containing
 * a single {@code bookmarks.json} file. The {@code updated_at} field returned by the Gist API
 * is used as an optimistic-lock token (etag) to detect concurrent modifications.
 */
public class GithubRemoteBookmarksStore extends AbstractRemoteBookmarksStore {
    private final Project project;
    private final GithubConnectionManager connectionManager;
    private final GistMappingsStore gistMappingsStore;
    private final GistChangeManager gistChangeManager;
    private final GistMappingPropertiesProvider propertiesProvider;

    public GithubRemoteBookmarksStore(Project project,
                                       GithubConnectionManager connectionManager,
                                       GistMappingsStore gistMappingsStore,
                                       GistChangeManager gistChangeManager) {
        super(project);
        this.project = project;
        this.connectionManager = connectionManager;
        this.gistMappingsStore = gistMappingsStore;
        this.gistChangeManager = gistChangeManager;
        this.propertiesProvider = new GistMappingPropertiesProvider();
    }

    private IGistApiClient getApiClient() {
        return connectionManager.getGistApiClient();
    }

    @Override
    public void init(RemoteBookmarksStoreDescriptor descriptor) {
        super.init(descriptor);

        MessageBusConnection messageBusConnection = project.getMessageBus().connect(this);

        messageBusConnection.subscribe(GithubConnectionListener.TOPIC, new GithubConnectionListener() {
            @Override
            public void connected() {
                postConnected();
            }

            @Override
            public void disconnected() {
                postDisconnected();
            }
        });

        messageBusConnection.subscribe(IGistMappingsListener.TOPIC, new IGistMappingsListener() {
            @Override
            public void mappingAdded(BookmarkId bookmarkFolderId) {
                postMappingAdded(bookmarkFolderId);
            }

            @Override
            public void mappingRemoved(BookmarkId bookmarkFolderId) {
                postMappingRemoved(bookmarkFolderId);
            }
        });

        messageBusConnection.subscribe(IGistChangeListener.TOPIC,
                (bookmarkFolderId, gistId) -> postRemoteBookmarksTreeChanged(bookmarkFolderId));

        gistChangeManager.init();
        Disposer.register(this, gistChangeManager);
    }

    @Override
    public void dispose() {
        // gistChangeManager and messageBusConnection are disposed automatically
    }

    @Override
    public void connect(ProgressIndicator indicator) throws IOException {
        connectionManager.connect(indicator);
    }

    @Override
    public void disconnect(ProgressIndicator indicator) {
        connectionManager.disconnect(indicator);
    }

    @Override
    public State getState() {
        return connectionManager.getState();
    }

    @Override
    public RemoteBookmarksTree add(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId,
                                   ProgressIndicator indicator) throws IOException {
        assertConnected();
        if (indicator != null) {
            indicator.setText("Saving bookmark folder to GitHub Gist");
            indicator.setFraction(0.0);
        }
        BookmarkFolder folder = requireFolder(bookmarksTree, bookmarkFolderId);
        byte[] content = serializeBookmarkFolder(bookmarksTree, bookmarkFolderId);
        if (indicator != null) {
            indicator.setFraction(0.2);
        }
        GistResponse response = new CreateGistOperation(getApiClient())
                .createGist(folder.getPropertyValue(Bookmark.PROPERTY_NAME), content, indicator);
        BookmarksTree subTree = bookmarksTree.subTree(bookmarkFolderId);
        gistMappingsStore.add(bookmarkFolderId, response.id,
                propertiesProvider.getProperties(response, subTree));
        if (indicator != null) {
            indicator.setFraction(1.0);
        }
        return new RemoteBookmarksTree(this, subTree, response.updated_at);
    }

    @Override
    public void remove(BookmarkId bookmarkFolderId, ProgressIndicator indicator) throws IOException {
        assertConnected();
        if (indicator != null) {
            indicator.setText("Removing bookmark folder from GitHub Gists");
            indicator.setFraction(0.0);
        }
        String gistId = requireGistId(bookmarkFolderId);
        gistMappingsStore.remove(bookmarkFolderId);
        if (indicator != null) {
            indicator.setFraction(0.1);
        }
        new DeleteGistOperation(getApiClient()).deleteGist(gistId, indicator);
        if (indicator != null) {
            indicator.setFraction(1.0);
        }
    }

    @Override
    public RemoteBookmarksTree load(BookmarkId bookmarkFolderId, ProgressIndicator indicator) throws IOException {
        assertConnected();
        if (indicator != null) {
            indicator.setText("Loading bookmark folder from GitHub Gist");
            indicator.setFraction(0.0);
        }
        String gistId = requireGistId(bookmarkFolderId);
        LoadGistOperation.GistContents contents =
                new LoadGistOperation(getApiClient()).loadGist(gistId, indicator);
        if (indicator != null) {
            indicator.setFraction(0.8);
        }
        IBookmarksTreeDeserializer deserializer = new BookmarksTreeJsonDeserializer();
        BookmarksTree subTree = deserializer.deserialize(
                new StringReader(new String(contents.content(), StandardCharsets.UTF_8)));
        gistMappingsStore.update(gistId, propertiesProvider.getProperties(contents.response(), subTree));
        if (indicator != null) {
            indicator.setFraction(1.0);
        }
        return new RemoteBookmarksTree(this, subTree, contents.etag());
    }

    @Override
    public RemoteBookmarksTree save(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId,
                                    String etag, ProgressIndicator indicator)
            throws IOException, ConflictException {
        assertConnected();
        if (indicator != null) {
            indicator.setText("Saving bookmark folder to GitHub Gist");
            indicator.setFraction(0.0);
        }
        String gistId = requireGistId(bookmarkFolderId);
        byte[] content = serializeBookmarkFolder(bookmarksTree, bookmarkFolderId);
        if (indicator != null) {
            indicator.setFraction(0.2);
        }
        GistResponse response = new UpdateGistOperation(getApiClient())
                .updateGist(gistId, content, etag, indicator);
        BookmarksTree subTree = bookmarksTree.subTree(bookmarkFolderId);
        gistMappingsStore.update(gistId, propertiesProvider.getProperties(response, subTree));
        if (indicator != null) {
            indicator.setFraction(1.0);
        }
        return new RemoteBookmarksTree(this, subTree, response.updated_at);
    }

    @Override
    public Set<RemoteBookmarkFolder> getRemoteBookmarkFolders() {
        return gistMappingsStore.getMappings().stream()
                .map(m -> new RemoteBookmarkFolder(getDescriptor().id(), m.getBookmarkFolderId(), m.getProperties()))
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<RemoteBookmarkFolder> getRemoteBookmarkFolder(BookmarkId bookmarkFolderId) {
        return gistMappingsStore.getMapping(bookmarkFolderId)
                .map(m -> new RemoteBookmarkFolder(getDescriptor().id(), m.getBookmarkFolderId(), m.getProperties()));
    }

    @Override
    public UserInfo getUserInfo() {
        return connectionManager.getUserInfo();
    }

    @Override
    public void deleteCredentials() {
        // Token is managed by the IntelliJ GitHub plugin — nothing to delete here.
    }

    private void assertConnected() {
        if (connectionManager.getState() != State.connected) {
            throw new IllegalStateException("Not connected to GitHub");
        }
    }

    private BookmarkFolder requireFolder(BookmarksTree tree, BookmarkId bookmarkFolderId) {
        Bookmark bookmark = tree.getBookmark(bookmarkFolderId);
        if (!(bookmark instanceof BookmarkFolder folder)) {
            throw new IllegalArgumentException("Cannot find folder with id " + bookmarkFolderId);
        }
        return folder;
    }

    private String requireGistId(BookmarkId bookmarkFolderId) {
        return gistMappingsStore.getMapping(bookmarkFolderId)
                .map(GistMapping::getGistId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Folder not added to GitHub Gists: " + bookmarkFolderId));
    }

    private byte[] serializeBookmarkFolder(BookmarksTree tree, BookmarkId bookmarkFolderId) throws IOException {
        IBookmarksTreeSerializer serializer = new BookmarksTreeJsonSerializer(true);
        StringWriter writer = new StringWriter();
        serializer.serialize(tree, bookmarkFolderId, writer);
        return writer.getBuffer().toString().getBytes(StandardCharsets.UTF_8);
    }
}
