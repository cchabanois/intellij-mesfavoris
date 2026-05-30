package mesfavoris.github.mcp

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.projectOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.Serializable
import mesfavoris.github.BookmarksGithubService
import mesfavoris.github.dialogs.AddGistLinkDialog
import mesfavoris.github.mappings.GistMappingsStore
import mesfavoris.github.operations.GistApiClient
import mesfavoris.github.operations.ImportGistOperation
import mesfavoris.model.BookmarkFolder
import mesfavoris.model.BookmarkId
import mesfavoris.remote.IRemoteBookmarksStore
import mesfavoris.service.IBookmarksService
import java.io.IOException
import java.net.http.HttpClient

class MesFavorisGithubMcpToolset : McpToolset {

    private suspend fun currentProject(): Project =
        runCatching { currentCoroutineContext().projectOrNull }.getOrNull()
            ?: ProjectManager.getInstance().openProjects.firstOrNull()
            ?: mcpFail("No active project")

    private suspend fun bookmarksService(): IBookmarksService =
        currentProject().getService(IBookmarksService::class.java)
            ?: mcpFail("Bookmarks service unavailable")

    private suspend fun gistMappingsStore(): GistMappingsStore =
        currentProject().getService(GistMappingsStore::class.java)
            ?: mcpFail("Gist mappings store unavailable")

    private suspend fun requireConnected(): GistApiClient {
        val project = currentProject()
        val service = project.getService(BookmarksGithubService::class.java)
            ?: mcpFail("GitHub service unavailable")
        val connectionManager = service.connectionManager
        if (connectionManager.state != IRemoteBookmarksStore.State.connected) {
            mcpFail("Not connected to GitHub. Use connect_remote_store with storeId 'github' first.")
        }
        val token = connectionManager.accessToken
        val baseUrl = connectionManager.apiBaseUrl
        return GistApiClient({ token }, { baseUrl }, HttpClient.newHttpClient())
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

    @McpTool
    @McpDescription(description = "List GitHub Gists bookmark files available for import. Returns gists containing bookmarks.json with their ID, description, and whether they are already imported.")
    suspend fun list_github_bookmark_gists(): GistListResult {
        val apiClient = requireConnected()
        val mappingsStore = gistMappingsStore()
        return try {
            val gists = apiClient.listGists()
                .filter { it.files != null && it.files.containsKey("bookmarks.json") }
                .map { gist ->
                    GistResult(
                        gistId = gist.id,
                        description = gist.description ?: "",
                        url = gist.html_url ?: "",
                        alreadyImported = mappingsStore.getMapping(gist.id).isPresent
                    )
                }
            GistListResult(gists)
        } catch (e: IOException) {
            mcpFail("Could not list GitHub Gists: ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "Import a GitHub Gist bookmark file by its gist ID. The gist ID can be obtained from list_github_bookmark_gists.")
    suspend fun import_github_bookmark_gist(
        @McpDescription(description = "The GitHub Gist ID to import") gistId: String,
        @McpDescription(description = "Parent bookmark folder ID (default: auto-placed)") parentId: String = ""
    ): String {
        if (gistId.isBlank()) mcpFail("Gist ID cannot be blank")
        val apiClient = requireConnected()
        val mappingsStore = gistMappingsStore()
        val bookmarksService = bookmarksService()
        val resolvedParentId = resolveParentId(parentId)
        return try {
            ImportGistOperation(apiClient, mappingsStore, bookmarksService)
                .importGist(resolvedParentId, gistId, null)
            "Imported GitHub Gist: $gistId"
        } catch (e: Exception) {
            mcpFail("Could not import gist '$gistId': ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "Import a GitHub Gist bookmark file from a sharing URL (e.g. https://gist.github.com/user/abc123...).")
    suspend fun import_github_bookmark_from_url(
        @McpDescription(description = "The GitHub Gist sharing URL") url: String,
        @McpDescription(description = "Parent bookmark folder ID (default: auto-placed)") parentId: String = ""
    ): String {
        if (url.isBlank()) mcpFail("URL cannot be blank")
        val gistId = AddGistLinkDialog.extractGistId(url)
            ?: mcpFail("Could not extract a GitHub Gist ID from: $url")
        return import_github_bookmark_gist(gistId, parentId)
    }

    @Serializable
    data class GistResult(
        @property:McpDescription("GitHub Gist ID") val gistId: String,
        @property:McpDescription("Gist description") val description: String,
        @property:McpDescription("GitHub Gist URL") val url: String,
        @property:McpDescription("Whether this gist is already imported as a bookmark folder") val alreadyImported: Boolean
    )

    @Serializable
    data class GistListResult(
        @property:McpDescription("List of GitHub Gist bookmark files") val gists: List<GistResult>
    )
}
