package mesfavoris.remote.internal.mcp

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.projectOrNull
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mesfavoris.BookmarksException
import mesfavoris.internal.mcp.buildFolderPath
import mesfavoris.model.Bookmark
import mesfavoris.remote.IRemoteBookmarksStore
import mesfavoris.remote.RemoteBookmarksStoreManager
import mesfavoris.service.IBookmarksService
import java.io.IOException

class RemoteStoresMcpToolset : McpToolset {

    private suspend fun currentProject(): Project =
        runCatching { currentCoroutineContext().projectOrNull }.getOrNull()
            ?: ProjectManager.getInstance().openProjects.firstOrNull()
            ?: mcpFail("No active project")

    private suspend fun bookmarksService(): IBookmarksService =
        currentProject().getService(IBookmarksService::class.java)
            ?: mcpFail("Bookmarks service unavailable")

    private suspend fun storeManager(): RemoteBookmarksStoreManager =
        currentProject().getService(RemoteBookmarksStoreManager::class.java)
            ?: mcpFail("Remote bookmarks store manager unavailable")

    private suspend fun requireStore(storeId: String): IRemoteBookmarksStore =
        storeManager().getRemoteBookmarksStore(storeId).orElse(null)
            ?: mcpFail("Remote store not found: $storeId")

    @McpTool
    @McpDescription(description = "List all available remote bookmark stores with their connection state and user info.")
    suspend fun list_remote_stores(): RemoteStoresResult {
        val manager = storeManager()
        return RemoteStoresResult(manager.remoteBookmarksStores.map { store ->
            store.toResult()
        })
    }

    @McpTool
    @McpDescription(description = "Connect to a remote bookmark store.")
    suspend fun connect_remote_store(
        @McpDescription(description = "The remote store ID") storeId: String
    ): String {
        val store = requireStore(storeId)
        if (store.state == IRemoteBookmarksStore.State.connected) {
            return "Already connected to store: $storeId"
        }
        return try {
            store.connect(EmptyProgressIndicator())
            "Connected to store: $storeId"
        } catch (e: IOException) {
            mcpFail("Could not connect to store '$storeId': ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "Disconnect from a remote bookmark store.")
    suspend fun disconnect_remote_store(
        @McpDescription(description = "The remote store ID") storeId: String
    ): String {
        val store = requireStore(storeId)
        if (store.state == IRemoteBookmarksStore.State.disconnected) {
            return "Already disconnected from store: $storeId"
        }
        return try {
            store.disconnect(EmptyProgressIndicator())
            "Disconnected from store: $storeId"
        } catch (e: IOException) {
            mcpFail("Could not disconnect from store '$storeId': ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "List all bookmark folders synchronized with a remote store.")
    suspend fun list_remote_folders(
        @McpDescription(description = "The remote store ID to filter by (default: all stores)") storeId: String = ""
    ): RemoteFoldersResult {
        val manager = storeManager()
        val service = bookmarksService()
        val tree = service.getBookmarksTree()

        val stores = if (storeId.isNotBlank()) {
            listOf(requireStore(storeId))
        } else {
            manager.remoteBookmarksStores.toList()
        }

        val folders = stores.flatMap { store ->
            store.remoteBookmarkFolders.map { remoteFolder ->
                val folderName = tree.getBookmark(remoteFolder.bookmarkFolderId)
                    ?.getPropertyValue(Bookmark.PROPERTY_NAME) ?: remoteFolder.bookmarkFolderId.toString()
                RemoteFolderResult(
                    storeId = remoteFolder.remoteBookmarkStoreId,
                    bookmarkFolderId = remoteFolder.bookmarkFolderId.toString(),
                    bookmarkFolderPath = buildFolderPath(tree, remoteFolder.bookmarkFolderId) + "/$folderName".let {
                        if (tree.getParentBookmark(remoteFolder.bookmarkFolderId) == null) "" else it
                    },
                    properties = remoteFolder.properties
                )
            }
        }
        return RemoteFoldersResult(folders)
    }

    @McpTool
    @McpDescription(description = "Synchronize a local bookmark folder with a remote store.")
    suspend fun add_to_remote_store(
        @McpDescription(description = "The remote store ID") storeId: String,
        @McpDescription(description = "The bookmark folder ID to synchronize") bookmarkFolderId: String
    ): String {
        val service = bookmarksService()
        val tree = service.getBookmarksTree()
        val id = mesfavoris.model.BookmarkId(bookmarkFolderId)
        val bookmark = tree.getBookmark(id) ?: mcpFail("Bookmark folder not found: $bookmarkFolderId")
        if (bookmark !is mesfavoris.model.BookmarkFolder) mcpFail("'$bookmarkFolderId' is not a folder")
        requireStore(storeId)
        return try {
            service.addToRemoteBookmarksStore(storeId, id, EmptyProgressIndicator())
            "Folder '$bookmarkFolderId' synchronized with store '$storeId'"
        } catch (e: BookmarksException) {
            mcpFail("Could not add folder to remote store: ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "Remove the synchronization between a local bookmark folder and a remote store.")
    suspend fun remove_from_remote_store(
        @McpDescription(description = "The remote store ID") storeId: String,
        @McpDescription(description = "The bookmark folder ID to unsynchronize") bookmarkFolderId: String
    ): String {
        val service = bookmarksService()
        requireStore(storeId)
        return try {
            service.removeFromRemoteBookmarksStore(storeId, mesfavoris.model.BookmarkId(bookmarkFolderId), EmptyProgressIndicator())
            "Folder '$bookmarkFolderId' removed from store '$storeId'"
        } catch (e: BookmarksException) {
            mcpFail("Could not remove folder from remote store: ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "Refresh bookmarks from a remote store. Fetches the latest changes from the remote.")
    suspend fun refresh_remote_store(
        @McpDescription(description = "The remote store ID to refresh (default: all stores)") storeId: String = ""
    ): String {
        val service = bookmarksService()
        return try {
            if (storeId.isNotBlank()) {
                requireStore(storeId)
                service.refresh(storeId, EmptyProgressIndicator())
                "Refreshed store: $storeId"
            } else {
                service.refresh(EmptyProgressIndicator())
                "Refreshed all remote stores"
            }
        } catch (e: BookmarksException) {
            mcpFail("Could not refresh: ${e.message}")
        }
    }

    private fun IRemoteBookmarksStore.toResult() = RemoteStoreResult(
        id = descriptor.id,
        label = descriptor.label,
        state = when (state) {
            IRemoteBookmarksStore.State.disconnected -> RemoteStoreState.DISCONNECTED
            IRemoteBookmarksStore.State.connecting   -> RemoteStoreState.CONNECTING
            IRemoteBookmarksStore.State.connected    -> RemoteStoreState.CONNECTED
        },
        userEmail = userInfo?.emailAddress,
        userDisplayName = userInfo?.displayName
    )

    @Serializable
    enum class RemoteStoreState {
        @SerialName("disconnected") DISCONNECTED,
        @SerialName("connecting")   CONNECTING,
        @SerialName("connected")    CONNECTED
    }

    @Serializable
    data class RemoteStoreResult(
        @property:McpDescription("Store ID (e.g. 'gdrive')") val id: String,
        @property:McpDescription("Human-readable store label") val label: String,
        @property:McpDescription("Connection state: disconnected, connecting, or connected") val state: RemoteStoreState,
        @property:McpDescription("Email address of the connected user") val userEmail: String? = null,
        @property:McpDescription("Display name of the connected user") val userDisplayName: String? = null
    )

    @Serializable
    data class RemoteStoresResult(
        @property:McpDescription("The list of remote bookmark stores") val stores: List<RemoteStoreResult>
    )

    @Serializable
    data class RemoteFolderResult(
        @property:McpDescription("ID of the remote store this folder belongs to") val storeId: String,
        @property:McpDescription("Local bookmark folder ID") val bookmarkFolderId: String,
        @property:McpDescription("Path of the bookmark folder from root") val bookmarkFolderPath: String,
        @property:McpDescription("Remote folder properties (e.g. readonly, bookmarksCount)") val properties: Map<String, String>
    )

    @Serializable
    data class RemoteFoldersResult(
        @property:McpDescription("The list of synchronized remote bookmark folders") val folders: List<RemoteFolderResult>
    )
}
