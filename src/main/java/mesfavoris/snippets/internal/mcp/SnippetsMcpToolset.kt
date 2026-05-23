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
import mesfavoris.internal.mcp.McpBookmarkProperties
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
    @McpDescription(description = "Add a snippet bookmark storing a code excerpt for quick reference. Navigating to the bookmark copies its content to the clipboard.")
    suspend fun add_snippet_bookmark(
        @McpDescription(description = "The code snippet to store.") content: String,
        @McpDescription(description = "Name for the bookmark. Defaults to the first non-empty line of content if blank.") name: String = "",
        @McpDescription(description = "Optional comment. Only the first line is shown in the bookmark tree; the full multi-line comment is visible in the details panel.") comment: String = "",
        @McpDescription(description = "ID of the parent bookmark folder (default: currently selected folder, then 'default' folder, then root). The 'default' folder is a general-purpose inbox for temporary or yet-to-be-organized bookmarks.") parentId: String = ""
    ): BookmarkResult {
        if (content.isBlank()) mcpFail("Snippet content must not be blank")

        return createSnippetBookmark(content, name, comment, parentId)
    }

    @McpTool
    @McpDescription(description = "Add a command bookmark storing a single shell command. Navigating to the bookmark copies the command to the clipboard, ready to paste in a terminal. Store exactly one command per bookmark — call this tool once per command.")
    suspend fun add_command_bookmark(
        @McpDescription(description = "The shell command to store. Must be a single command with no inline # comments and no newlines.") command: String,
        @McpDescription(description = "Name for the bookmark. Defaults to the command itself if blank.") name: String = "",
        @McpDescription(description = "Optional comment describing what the command does. Only the first line is shown in the bookmark tree; the full multi-line comment is visible in the details panel.") comment: String = "",
        @McpDescription(description = "ID of the parent bookmark folder (default: currently selected folder, then 'default' folder, then root). The 'default' folder is a general-purpose inbox for temporary or yet-to-be-organized bookmarks.") parentId: String = ""
    ): BookmarkResult {
        if (command.isBlank()) mcpFail("Command must not be blank")
        if (command.trimStart().startsWith('#')) mcpFail("Command must not start with a comment. Put the description in the name or comment fields instead.")
        val nonEmptyLines = command.lines().filter { it.isNotBlank() }
        val hasMultipleCommands = nonEmptyLines.size > 1 &&
            nonEmptyLines.zipWithNext().any { (prev, _) ->
                val trimmed = prev.trimEnd()
                !trimmed.endsWith('\\') && !trimmed.endsWith('|') &&
                !trimmed.endsWith("&&") && !trimmed.endsWith("||")
            }
        if (hasMultipleCommands) mcpFail("A command bookmark must contain a single command. Call this tool once per command.")

        return createSnippetBookmark(command, name.ifBlank { command }, comment, parentId)
    }

    private suspend fun createSnippetBookmark(
        content: String,
        name: String,
        comment: String,
        parentId: String
    ): BookmarkResult {
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
            put(McpBookmarkProperties.PROPERTY_ORIGIN, McpBookmarkProperties.ORIGIN_MCP)
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
