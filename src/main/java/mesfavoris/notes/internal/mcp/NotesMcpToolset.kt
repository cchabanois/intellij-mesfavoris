package mesfavoris.notes.internal.mcp

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
import mesfavoris.notes.NoteBookmarkProperties.PROP_NOTES
import mesfavoris.service.IBookmarksService

class NotesMcpToolset : McpToolset {

    private suspend fun currentProject(): Project =
        runCatching { currentCoroutineContext().projectOrNull }.getOrNull()
            ?: ProjectManager.getInstance().openProjects.firstOrNull()
            ?: mcpFail("No active project")

    private suspend fun bookmarksService(): IBookmarksService =
        currentProject().getService(IBookmarksService::class.java)
            ?: mcpFail("Bookmarks service unavailable")

    @McpTool
    @McpDescription(description = "Add a note bookmark (favori) storing Markdown text content.")
    suspend fun add_note(
        @McpDescription(description = "Name of the note bookmark. Defaults to the first heading or first line of content if blank.") name: String = "",
        @McpDescription(description = "Note content in Markdown format") content: String = "",
        @McpDescription(description = "Optional comment") comment: String = "",
        @McpDescription(description = "ID of the parent bookmark folder (default: currently selected folder, then 'default' folder, then root)") parentId: String = ""
    ): BookmarkResult {
        val service = bookmarksService()

        val resolvedParentId: BookmarkId? = if (parentId.isNotBlank()) {
            val id = BookmarkId(parentId)
            val bookmark = service.getBookmarksTree().getBookmark(id) ?: mcpFail("Parent folder not found: $parentId")
            if (bookmark !is BookmarkFolder) mcpFail("'$parentId' is not a folder")
            id
        } else null

        val resolvedName = name.ifBlank { deriveName(content) }
        val properties = buildMap {
            put(Bookmark.PROPERTY_NAME, resolvedName)
            put(PROP_NOTES, content)
            if (comment.isNotBlank()) put(Bookmark.PROPERTY_COMMENT, comment)
        }

        return try {
            val newId = service.addBookmark(properties, resolvedParentId)
            val updatedTree = service.getBookmarksTree()
            val bookmark = updatedTree.getBookmark(newId)
                ?: mcpFail("Note bookmark was created but could not be retrieved")
            bookmarkToResult(updatedTree, bookmark)
        } catch (e: BookmarksException) {
            mcpFail("Could not create note bookmark: ${e.message}")
        }
    }

    private fun deriveName(content: String): String {
        if (content.isBlank()) return "New Note"
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                return trimmed.trimStart('#').trim().ifEmpty { "New Note" }
            }
        }
        return "New Note"
    }
}
