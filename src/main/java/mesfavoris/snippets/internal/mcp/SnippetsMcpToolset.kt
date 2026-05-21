package mesfavoris.snippets.internal.mcp

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
import mesfavoris.snippets.SnippetBookmarkProperties.PROP_SNIPPET_CONTENT
import mesfavoris.snippets.internal.SnippetBookmarkPropertiesProvider

class SnippetsMcpToolset : McpToolset {

    private suspend fun currentProject(): Project =
        runCatching { currentCoroutineContext().projectOrNull }.getOrNull()
            ?: ProjectManager.getInstance().openProjects.firstOrNull()
            ?: mcpFail("No active project")

    private suspend fun bookmarksService(): IBookmarksService =
        currentProject().getService(IBookmarksService::class.java)
            ?: mcpFail("Bookmarks service unavailable")

    @McpTool
    @McpDescription(description = "Add a snippet bookmark (favori) storing text content such as a shell command or code snippet.")
    suspend fun add_snippet(
        @McpDescription(description = "The snippet content to store (e.g. a shell command or code snippet)") content: String,
        @McpDescription(description = "Name for the bookmark. Defaults to the first non-empty line of content if blank.") name: String = "",
        @McpDescription(description = "Optional comment") comment: String = "",
        @McpDescription(description = "ID of the parent bookmark folder (default: currently selected folder, then 'default' folder, then root)") parentId: String = ""
    ): BookmarkResult {
        if (content.isBlank()) mcpFail("Snippet content must not be blank")

        val service = bookmarksService()

        val resolvedParentId: BookmarkId? = if (parentId.isNotBlank()) {
            val id = BookmarkId(parentId)
            val bookmark = service.getBookmarksTree().getBookmark(id) ?: mcpFail("Parent folder not found: $parentId")
            if (bookmark !is BookmarkFolder) mcpFail("'$parentId' is not a folder")
            id
        } else null

        val resolvedName = name.ifBlank { SnippetBookmarkPropertiesProvider.getName(content) }
        val properties = buildMap {
            put(Bookmark.PROPERTY_NAME, resolvedName)
            put(PROP_SNIPPET_CONTENT, content)
            if (comment.isNotBlank()) put(Bookmark.PROPERTY_COMMENT, comment)
        }

        return try {
            val newId = service.addBookmark(properties, resolvedParentId)
            val updatedTree = service.getBookmarksTree()
            val bookmark = updatedTree.getBookmark(newId)
                ?: mcpFail("Snippet bookmark was created but could not be retrieved")
            bookmarkToResult(updatedTree, bookmark)
        } catch (e: BookmarksException) {
            mcpFail("Could not create snippet bookmark: ${e.message}")
        }
    }
}
