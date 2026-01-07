package mesfavoris.service;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.BookmarksException;
import mesfavoris.IBookmarksMarkers;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksDirtyStateTracker;
import mesfavoris.recent.IRecentBookmarksProvider;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarkFolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for managing bookmarks in a project.
 * This interface provides the public API for bookmark operations.
 */
public interface IBookmarksService {

    /**
     * Get the bookmark database
     */
    BookmarkDatabase getBookmarkDatabase();

    /**
     * Get the bookmarks tree
     */
    BookmarksTree getBookmarksTree();

    /**
     * Get the bookmarks markers
     */
    IBookmarksMarkers getBookmarksMarkers();

    /**
     * Get the bookmarks dirty state tracker
     */
    IBookmarksDirtyStateTracker getBookmarksDirtyStateTracker();

    /**
     * Get the bookmark label provider
     */
    IBookmarkLabelProvider getBookmarkLabelProvider();

    /**
     * Get the recent bookmarks provider
     */
    IRecentBookmarksProvider getRecentBookmarksProvider();

    /**
     * Navigate to a bookmark
     *
     * @param bookmarkId the bookmark ID
     * @param progress   the progress indicator
     * @throws BookmarksException if the bookmark cannot be found or navigated to
     */
    void gotoBookmark(BookmarkId bookmarkId, ProgressIndicator progress) throws BookmarksException;

    /**
     * Add a bookmark from the current context
     *
     * @param dataContext the data context
     * @param progress    the progress indicator
     * @return the ID of the created bookmark
     * @throws BookmarksException if the bookmark cannot be created
     */
    BookmarkId addBookmark(DataContext dataContext, ProgressIndicator progress) throws BookmarksException;

    /**
     * Add a bookmark folder
     *
     * @param parentFolderId the parent folder ID
     * @param folderName     the folder name
     * @throws BookmarksException if the folder cannot be created
     */
    BookmarkId addBookmarkFolder(BookmarkId parentFolderId, String folderName) throws BookmarksException;

    /**
     * Delete bookmarks
     *
     * @param selection the bookmark IDs to delete
     * @param recurse   whether to delete recursively
     * @throws BookmarksException if the bookmarks cannot be deleted
     */
    void deleteBookmarks(List<BookmarkId> selection, boolean recurse) throws BookmarksException;

    /**
     * Rename a bookmark
     *
     * @param bookmarkId the bookmark ID
     * @param newName    the new name
     * @throws BookmarksException if the bookmark cannot be renamed
     */
    void renameBookmark(BookmarkId bookmarkId, String newName) throws BookmarksException;

    /**
     * Set bookmark properties
     *
     * @param bookmarkId the bookmark ID
     * @param properties the new properties
     * @throws BookmarksException if the properties cannot be set
     */
    void setBookmarkProperties(BookmarkId bookmarkId, Map<String, String> properties) throws BookmarksException;

    /**
     * Copy bookmarks to clipboard
     *
     * @param selection the bookmark IDs to copy
     */
    void copyToClipboard(List<BookmarkId> selection);

    /**
     * Paste bookmarks from clipboard
     *
     * @param parentBookmarkId the parent bookmark ID
     * @param progress         the progress indicator
     * @throws BookmarksException if the bookmarks cannot be pasted
     */
    void paste(BookmarkId parentBookmarkId, ProgressIndicator progress) throws BookmarksException;

    /**
     * Paste bookmarks from clipboard after a specific bookmark
     *
     * @param parentBookmarkId the parent bookmark ID
     * @param bookmarkId       the bookmark ID to paste after
     * @param progress         the progress indicator
     * @throws BookmarksException if the bookmarks cannot be pasted
     */
    void pasteAfter(BookmarkId parentBookmarkId, BookmarkId bookmarkId, ProgressIndicator progress) throws BookmarksException;

    /**
     * Cut bookmarks to clipboard
     *
     * @param selection the bookmark IDs to cut
     * @throws BookmarksException if the bookmarks cannot be cut
     */
    void cutToClipboard(List<BookmarkId> selection) throws BookmarksException;

    /**
     * Add a bookmarks tree
     *
     * @param parentBookmarkId    the parent bookmark ID
     * @param sourceBookmarksTree the source bookmarks tree
     * @param afterCommit         callback to execute after commit
     * @throws BookmarksException if the tree cannot be added
     */
    void addBookmarksTree(BookmarkId parentBookmarkId, BookmarksTree sourceBookmarksTree,
                          Consumer<BookmarksTree> afterCommit) throws BookmarksException;

    /**
     * Add to remote bookmarks store
     *
     * @param storeId          the store ID
     * @param bookmarkFolderId the bookmark folder ID
     * @param monitor          the progress indicator
     * @throws BookmarksException if the operation fails
     */
    void addToRemoteBookmarksStore(String storeId, BookmarkId bookmarkFolderId,
                                   ProgressIndicator monitor) throws BookmarksException;

    /**
     * Remove from remote bookmarks store
     *
     * @param storeId          the store ID
     * @param bookmarkFolderId the bookmark folder ID
     * @param monitor          the progress indicator
     * @throws BookmarksException if the operation fails
     */
    void removeFromRemoteBookmarksStore(String storeId, BookmarkId bookmarkFolderId,
                                   ProgressIndicator monitor) throws BookmarksException;

    /**
     * Get the remote bookmark folder for a given bookmark folder ID
     *
     * @param bookmarkFolderId the bookmark folder ID
     * @return the remote bookmark folder, or empty if not found
     */
    Optional<RemoteBookmarkFolder> getRemoteBookmarkFolder(BookmarkId bookmarkFolderId);

    /**
     * Get the remote bookmarks store by ID
     *
     * @param storeId the store ID
     * @return the remote bookmarks store, or empty if not found
     */
    Optional<IRemoteBookmarksStore> getRemoteBookmarksStore(String storeId);

    /**
     * Select a bookmark in the bookmarks tree view
     *
     * @param bookmarkId the bookmark to select
     */
    void selectBookmarkInTree(BookmarkId bookmarkId);

    /**
     * Refresh a specific remote bookmark folder
     *
     * @param bookmarkFolderId the bookmark folder ID to refresh
     * @param progress         the progress indicator
     * @throws BookmarksException if the operation fails
     */
    void refresh(BookmarkId bookmarkFolderId, ProgressIndicator progress) throws BookmarksException;

    /**
     * Refresh all remote bookmark folders
     *
     * @param progress the progress indicator
     * @throws BookmarksException if the operation fails
     */
    void refresh(ProgressIndicator progress) throws BookmarksException;

    /**
     * Refresh all remote bookmark folders for a specific store
     *
     * @param storeId  the store ID
     * @param progress the progress indicator
     * @throws BookmarksException if the operation fails
     */
    void refresh(String storeId, ProgressIndicator progress) throws BookmarksException;

    /**
     * Update a bookmark from the current context
     *
     * @param bookmarkId  the bookmark ID to update
     * @param dataContext the data context
     * @param progress    the progress indicator
     * @throws BookmarksException if the bookmark cannot be updated
     */
    void updateBookmark(BookmarkId bookmarkId, DataContext dataContext, ProgressIndicator progress) throws BookmarksException;

}

