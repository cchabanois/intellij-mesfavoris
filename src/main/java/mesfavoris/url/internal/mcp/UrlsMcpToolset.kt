package mesfavoris.url.internal.mcp

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.projectOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.currentCoroutineContext
import mesfavoris.BookmarksException
import mesfavoris.internal.mcp.BookmarkResult
import mesfavoris.internal.mcp.bookmarkToResult
import mesfavoris.model.Bookmark
import mesfavoris.model.BookmarkFolder
import mesfavoris.model.BookmarkId
import mesfavoris.service.IBookmarksService
import mesfavoris.url.UrlBookmarkProperties.PROP_URL

class UrlsMcpToolset : McpToolset {

    private suspend fun currentProject(): Project =
        runCatching { currentCoroutineContext().projectOrNull }.getOrNull()
            ?: ProjectManager.getInstance().openProjects.firstOrNull()
            ?: mcpFail("No active project")

    private suspend fun bookmarksService(): IBookmarksService =
        currentProject().getService(IBookmarksService::class.java)
            ?: mcpFail("Bookmarks service unavailable")

    @McpTool
    @McpDescription(description = "Add a URL bookmark (favori) for a web page or any URL.")
    suspend fun add_url_bookmark(
        @McpDescription(description = "The URL to bookmark") url: String,
        @McpDescription(description = "Name for the bookmark. Defaults to the URL if blank.") name: String = "",
        @McpDescription(description = "Optional comment") comment: String = "",
        @McpDescription(description = "ID of the parent bookmark folder (default: currently selected folder, then 'default' folder, then root)") parentId: String = ""
    ): BookmarkResult {
        if (url.isBlank()) mcpFail("URL must not be blank")

        val service = bookmarksService()

        val resolvedParentId: BookmarkId? = if (parentId.isNotBlank()) {
            val id = BookmarkId(parentId)
            val bookmark = service.getBookmarksTree().getBookmark(id) ?: mcpFail("Parent folder not found: $parentId")
            if (bookmark !is BookmarkFolder) mcpFail("'$parentId' is not a folder")
            id
        } else null

        val resolvedName = name.ifBlank { url }
        val properties = buildMap {
            put(Bookmark.PROPERTY_NAME, resolvedName)
            put(PROP_URL, url)
            if (comment.isNotBlank()) put(Bookmark.PROPERTY_COMMENT, comment)
        }

        return try {
            val newId = service.addBookmark(properties, resolvedParentId)
            val updatedTree = service.getBookmarksTree()
            val bookmark = updatedTree.getBookmark(newId)
                ?: mcpFail("URL bookmark was created but could not be retrieved")
            bookmarkToResult(updatedTree, bookmark)
        } catch (e: BookmarksException) {
            mcpFail("Could not create URL bookmark: ${e.message}")
        }
    }
}
