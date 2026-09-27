package mesfavoris.remote;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public interface IRemoteBookmarksStore extends Disposable {

    enum State {
        disconnected, connecting, connected
    }

    /**
     * User info about the user associated to this remote bookmark store
     *
     * @return the user info or null if unknown
     */
    UserInfo getUserInfo();

    RemoteBookmarksStoreDescriptor getDescriptor();

    void connect(ProgressIndicator progressIndicator) throws IOException;

    void disconnect(ProgressIndicator progressIndicator) throws IOException;

    State getState();

    RemoteBookmarksTree add(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId, ProgressIndicator progressIndicator)
            throws IOException;

    void remove(BookmarkId bookmarkFolderId, ProgressIndicator progressIndicator) throws IOException;

    Set<RemoteBookmarkFolder> getRemoteBookmarkFolders();

    Optional<RemoteBookmarkFolder> getRemoteBookmarkFolder(BookmarkId bookmarkFolderId);

    RemoteBookmarksTree load(BookmarkId bookmarkFolderId, ProgressIndicator progressIndicator) throws IOException;

    /**
     * @param etag the etag of the remote version the tree is based on, or null to save without a conflict check
     * @throws ConflictException if the remote version has changed since {@code etag}
     */
    RemoteBookmarksTree save(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId, String etag,
                             ProgressIndicator progressIndicator) throws IOException, ConflictException;

    /**
     * Delete stored credentials for this remote bookmarks store.
     *
     * @throws IOException if the operation fails
     */
    void deleteCredentials() throws IOException;

}
