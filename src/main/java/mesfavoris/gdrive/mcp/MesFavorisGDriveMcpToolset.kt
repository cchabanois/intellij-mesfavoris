package mesfavoris.gdrive.mcp

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.projectOrNull
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.Serializable
import mesfavoris.gdrive.BookmarksGDriveService
import mesfavoris.gdrive.mappings.BookmarkMappingsStore
import mesfavoris.gdrive.operations.GetBookmarkFilesOperation
import mesfavoris.gdrive.operations.GetFileIdFromUrlOperation
import mesfavoris.gdrive.operations.ImportBookmarkFileOperation
import mesfavoris.model.BookmarkFolder
import mesfavoris.model.BookmarkId
import mesfavoris.remote.IRemoteBookmarksStore
import mesfavoris.service.IBookmarksService
import java.io.IOException
import java.util.*

class MesFavorisGDriveMcpToolset : McpToolset {

    private suspend fun currentProject(): Project =
        runCatching { currentCoroutineContext().projectOrNull }.getOrNull()
            ?: ProjectManager.getInstance().openProjects.firstOrNull()
            ?: mcpFail("No active project")

    private suspend fun bookmarksService(): IBookmarksService =
        currentProject().getService(IBookmarksService::class.java)
            ?: mcpFail("Bookmarks service unavailable")

    private suspend fun gdriveService(): BookmarksGDriveService =
        currentProject().getService(BookmarksGDriveService::class.java)
            ?: mcpFail("Google Drive service unavailable")

    private suspend fun mappingsStore(): BookmarkMappingsStore =
        currentProject().getService(BookmarkMappingsStore::class.java)
            ?: mcpFail("Bookmark mappings store unavailable")

    private suspend fun requireConnected(): BookmarksGDriveService {
        val service = gdriveService()
        if (service.connectionManager.state != IRemoteBookmarksStore.State.connected) {
            mcpFail("Not connected to Google Drive. Use connect_remote_store with storeId 'gdrive' first.")
        }
        return service
    }

    @McpTool
    @McpDescription(description = "List Google Drive bookmark files available for import. Returns files with their ID, title, and whether they are already imported.")
    suspend fun list_gdrive_bookmark_files(): GDriveFilesResult {
        val service = requireConnected()
        val mappings = mappingsStore()
        val drive = service.connectionManager.drive
            ?: mcpFail("Google Drive connection not initialized")
        return try {
            val files = GetBookmarkFilesOperation(drive).getBookmarkFiles()
            GDriveFilesResult(files.map { file ->
                GDriveFileResult(
                    fileId = file.id,
                    title = file.title ?: file.id,
                    alreadyImported = mappings.getMapping(file.id).isPresent
                )
            })
        } catch (e: IOException) {
            mcpFail("Could not list Google Drive bookmark files: ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "Import a Google Drive bookmark file by its file ID. The file ID can be obtained from list_gdrive_bookmark_files.")
    suspend fun import_gdrive_bookmark_file(
        @McpDescription(description = "The Google Drive file ID to import") fileId: String,
        @McpDescription(description = "Parent bookmark folder ID (default: auto-placed)") parentId: String = ""
    ): String {
        val service = requireConnected()
        val bookmarksService = bookmarksService()
        val mappings = mappingsStore()
        val connectionManager = service.connectionManager
        val drive = connectionManager.drive
            ?: mcpFail("Google Drive connection not initialized")

        val resolvedParentId = resolveParentId(parentId)
        val appFolderId = runCatching { connectionManager.applicationFolderId }.getOrNull()

        return try {
            val op = ImportBookmarkFileOperation(drive, mappings, bookmarksService, Optional.ofNullable(appFolderId))
            op.importBookmarkFile(resolvedParentId, fileId, EmptyProgressIndicator())
            "Imported Google Drive bookmark file: $fileId"
        } catch (e: Exception) {
            mcpFail("Could not import bookmark file '$fileId': ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "Import a Google Drive bookmark file from a sharing URL (e.g. https://drive.google.com/file/d/.../view).")
    suspend fun import_gdrive_bookmark_from_url(
        @McpDescription(description = "The Google Drive sharing URL") url: String,
        @McpDescription(description = "Parent bookmark folder ID (default: auto-placed)") parentId: String = ""
    ): String {
        if (url.isBlank()) mcpFail("URL cannot be blank")
        val fileId = GetFileIdFromUrlOperation().getFileId(url).orElse(null)
            ?: mcpFail("Could not extract a Google Drive file ID from URL: $url")
        return import_gdrive_bookmark_file(fileId, parentId)
    }

    private suspend fun resolveParentId(parentId: String): BookmarkId {
        if (parentId.isBlank()) {
            return bookmarksService().getBookmarksTree().rootFolder.id
        }
        val tree = bookmarksService().getBookmarksTree()
        val id = BookmarkId(parentId)
        val bookmark = tree.getBookmark(id) ?: mcpFail("Bookmark folder not found: $parentId")
        if (bookmark !is BookmarkFolder) mcpFail("'$parentId' is not a folder")
        return id
    }

    @Serializable
    data class GDriveFileResult(
        @property:McpDescription("Google Drive file ID") val fileId: String,
        @property:McpDescription("File title") val title: String,
        @property:McpDescription("Whether this file is already imported as a bookmark folder") val alreadyImported: Boolean
    )

    @Serializable
    data class GDriveFilesResult(
        @property:McpDescription("List of Google Drive bookmark files") val files: List<GDriveFileResult>
    )
}
