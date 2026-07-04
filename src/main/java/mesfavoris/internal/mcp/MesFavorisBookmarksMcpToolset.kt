package mesfavoris.internal.mcp

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.projectOrNull
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mesfavoris.BookmarksException
import mesfavoris.bookmarktype.BookmarkPropertyDescriptor
import mesfavoris.bookmarktype.IBookmarkPropertyDescriptors
import mesfavoris.internal.Constants.DEFAULT_BOOKMARKFOLDER_ID
import mesfavoris.internal.bookmarktypes.extension.ExtensionBookmarkPropertyDescriptors
import mesfavoris.internal.toolwindow.MesFavorisToolWindowUtils
import mesfavoris.model.Bookmark
import mesfavoris.model.BookmarkFolder
import mesfavoris.model.BookmarkId
import mesfavoris.service.IBookmarksService
import mesfavoris.service.MoveLocation

class MesFavorisBookmarksMcpToolset : McpToolset {

    private suspend fun currentProject(): Project =
        runCatching { currentCoroutineContext().projectOrNull }.getOrNull()
            ?: ProjectManager.getInstance().openProjects.firstOrNull()
            ?: mcpFail("No active project")

    private suspend fun bookmarksService(): IBookmarksService =
        currentProject().getService(IBookmarksService::class.java)
            ?: mcpFail("Bookmarks service unavailable")

    private fun propertyDescriptors(): IBookmarkPropertyDescriptors = ExtensionBookmarkPropertyDescriptors()

    /**
     * Parses the [returnProperties] tool parameter into the set of property keys to return.
     * `"*"` means "all properties except those marked excluded" (represented as null).
     */
    private fun parseReturnProperties(returnProperties: String): Set<String>? {
        val trimmed = returnProperties.trim()
        if (trimmed == "*") return null
        return trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    @McpTool
    @McpDescription(description = "List all known bookmark (favori) property descriptors, describing the possible properties of a bookmark.")
    suspend fun list_bookmark_properties(): PropertyDescriptorsResult {
        val descriptors = ExtensionBookmarkPropertyDescriptors()
        return PropertyDescriptorsResult(descriptors.getPropertyDescriptors().map { descriptor ->
            PropertyDescriptorResult(
                name = descriptor.name,
                type = when (descriptor.type) {
                    BookmarkPropertyDescriptor.BookmarkPropertyType.STRING  -> PropertyType.STRING
                    BookmarkPropertyDescriptor.BookmarkPropertyType.PATH    -> PropertyType.PATH
                    BookmarkPropertyDescriptor.BookmarkPropertyType.INT     -> PropertyType.INT
                    BookmarkPropertyDescriptor.BookmarkPropertyType.INSTANT -> PropertyType.INSTANT
                },
                updatable = descriptor.isUpdatable,
                excludedFromMcp = descriptor.isExcludedFromMcp,
                description = descriptor.description
            )
        })
    }

    @McpTool
    @McpDescription(description = "Navigate to a bookmark (favori) in the IDE by its ID.")
    suspend fun goto_bookmark(
        @McpDescription(description = "The bookmark ID to navigate to") id: String,
        @McpDescription(description = "Whether to also select the bookmark in the tree (default: true)") selectBookmark: Boolean = true
    ): String {
        val service = bookmarksService()
        return try {
            service.gotoBookmark(BookmarkId(id), EmptyProgressIndicator())
            if (selectBookmark) service.selectBookmarkInTree(BookmarkId(id))
            "Navigated to bookmark: $id"
        } catch (e: BookmarksException) {
            mcpFail("Could not navigate to bookmark '$id': ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "Show only specific bookmarks in the tree UI, hiding all others. The search bar shows 'Filtered by AI: <description>' — click × to restore the full view. Use this after finding relevant bookmarks with search_bookmarks or list_bookmark_folder.")
    suspend fun show_bookmarks(
        @McpDescription(description = "IDs of the bookmarks to display exclusively") ids: List<String>,
        @McpDescription(description = "Short description of what is being shown, e.g. 'caffeine-related snippets'. Displayed in the search bar.") description: String = ""
    ): String {
        if (ids.isEmpty()) mcpFail("Provide at least one bookmark ID")
        val service = bookmarksService()
        service.showOnlyBookmarks(ids.map { BookmarkId(it) }, description)
        return "Showing ${ids.size} bookmark(s)"
    }

    @McpTool
    @McpDescription(description = "Select a bookmark (favori) in the bookmarks tree view.")
    suspend fun select_bookmark(
        @McpDescription(description = "The bookmark ID to select") id: String
    ): String {
        val service = bookmarksService()
        service.getBookmarksTree().getBookmark(BookmarkId(id)) ?: mcpFail("Bookmark not found: $id")
        service.selectBookmarkInTree(BookmarkId(id))
        return "Selected bookmark: $id"
    }

    @McpTool
    @McpDescription(description = "Search bookmarks (favoris) by text.")
    suspend fun search_bookmarks(
        @McpDescription(description = "ID of the bookmark folder to search in (default: entire tree)") parentId: String = "",
        @McpDescription(description = "Text to search for (case-insensitive)") query: String,
        @McpDescription(description = "Whether to search recursively in subfolders (default: true)") recursive: Boolean = true,
        @McpDescription(description = "Comma-separated list of property names to search in (default: all properties)") attributes: String = "",
        @McpDescription(description = "Maximum number of results to return (default: 100)") maxResults: Int = 100,
        @McpDescription(description = "Comma-separated property names to include in each result (default: 'name'). Use '*' for all properties except large excluded ones (e.g. base64 icons); list an excluded property explicitly to force its inclusion.") returnProperties: String = "name"
    ): BookmarksResult {
        val service = bookmarksService()
        val tree = service.getBookmarksTree()
        val descriptors = propertyDescriptors()
        val requested = parseReturnProperties(returnProperties)
        val startFolderId = if (parentId.isNotBlank()) {
            val id = BookmarkId(parentId)
            val bookmark = tree.getBookmark(id) ?: mcpFail("Folder not found: $parentId")
            if (bookmark !is BookmarkFolder) mcpFail("'$parentId' is not a folder")
            id
        } else {
            tree.rootFolder.id
        }
        val candidates: Iterable<Bookmark> = if (recursive) tree.subTree(startFolderId)
                                             else tree.getChildren(startFolderId)

        val targetAttributes = if (attributes.isBlank()) emptyList()
                               else attributes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val lowerQuery = query.lowercase()

        val results = mutableListOf<BookmarkResult>()
        for (bookmark in candidates) {
            if (results.size >= maxResults) break
            if (bookmark.id == startFolderId) continue
            val props = bookmark.properties
            val matches = if (targetAttributes.isEmpty()) {
                props.values.any { it.lowercase().contains(lowerQuery) }
            } else {
                targetAttributes.any { attr -> props[attr]?.lowercase()?.contains(lowerQuery) == true }
            }
            if (!matches) continue
            results.add(bookmarkToResult(tree, bookmark, descriptors, requested))
        }
        return BookmarksResult(results)
    }

    @McpTool
    @McpDescription(description = "Create a bookmark folder. Always call list_bookmark_folder first to check the current state — the user may have added, moved or deleted bookmarks since the last interaction.")
    suspend fun create_bookmark_folder(
        @McpDescription(description = "Name of the bookmark folder to create (must not be blank)") name: String,
        @McpDescription(description = "ID of the parent bookmark folder (default: currently selected folder, then 'default' folder, then root). The 'default' folder is a general-purpose inbox for temporary or yet-to-be-organized bookmarks.") parentId: String = ""
    ): BookmarkResult {
        val project = currentProject()
        val service = bookmarksService()

        if (name.isBlank()) mcpFail("Folder name must not be blank")

        val tree = service.getBookmarksTree()
        val parentFolderId = if (parentId.isNotBlank()) {
            val id = BookmarkId(parentId)
            val bookmark = tree.getBookmark(id) ?: mcpFail("Parent folder not found: $parentId")
            if (bookmark !is BookmarkFolder) mcpFail("'$parentId' is not a folder")
            id
        } else {
            getSelectedFolderIdFromToolWindow(project)
                ?: if (tree.getBookmark(DEFAULT_BOOKMARKFOLDER_ID) != null) DEFAULT_BOOKMARKFOLDER_ID
                   else tree.rootFolder.id
        }

        return try {
            val newId = service.addBookmarkFolder(parentFolderId, name)
            service.modifyBookmark(newId, mapOf(McpBookmarkProperties.PROPERTY_ORIGIN to McpBookmarkProperties.ORIGIN_MCP))
            val updatedTree = service.getBookmarksTree()
            val folder = updatedTree.getBookmark(newId) ?: mcpFail("Folder was created but could not be retrieved")
            bookmarkToResult(updatedTree, folder, propertyDescriptors(), null)
        } catch (e: BookmarksException) {
            mcpFail("Could not create folder '$name': ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "List bookmarks (favoris) in a bookmark folder. Returns direct children, or all descendants if recursive. The bookmark tree can be modified by the user at any time outside of this conversation, so always verify the current state rather than relying on memory of previous interactions.")
    suspend fun list_bookmark_folder(
        @McpDescription(description = "ID of the bookmark folder to list (default: root folder)") folderId: String = "",
        @McpDescription(description = "Whether to list recursively (default: false)") recursive: Boolean = false,
        @McpDescription(description = "Comma-separated property names to include in each result (default: 'name'). Use '*' for all properties except large excluded ones (e.g. base64 icons); list an excluded property explicitly to force its inclusion.") returnProperties: String = "name"
    ): BookmarksResult {
        val service = bookmarksService()
        val tree = service.getBookmarksTree()
        val descriptors = propertyDescriptors()
        val requested = parseReturnProperties(returnProperties)
        val resolvedFolderId = if (folderId.isNotBlank()) {
            val id = BookmarkId(folderId)
            val bookmark = tree.getBookmark(id) ?: mcpFail("Folder not found: $folderId")
            if (bookmark !is BookmarkFolder) mcpFail("'$folderId' is not a folder")
            id
        } else {
            tree.rootFolder.id
        }
        val children: Iterable<Bookmark> = if (recursive) tree.subTree(resolvedFolderId)
                                           else tree.getChildren(resolvedFolderId)
        return BookmarksResult(children
            .filter { it.id != resolvedFolderId }
            .map { bookmarkToResult(tree, it, descriptors, requested) })
    }

    @McpTool
    @McpDescription(description = "Move bookmarks (favoris) to a new location. Returns a confirmation message.")
    suspend fun move_bookmarks(
        @McpDescription(description = "Comma-separated list of bookmark IDs to move") ids: String,
        @McpDescription(description = "Target bookmark ID") targetId: String,
        @McpDescription(description = "Where to place the bookmarks: INTO (into a bookmark folder), BEFORE or AFTER (relative to target)") location: String = "INTO"
    ): String {
        val service = bookmarksService()
        val bookmarkIds = ids.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { BookmarkId(it) }
        if (bookmarkIds.isEmpty()) mcpFail("No bookmark IDs provided")

        val loc = when (location.uppercase()) {
            "INTO"   -> MoveLocation.INTO
            "BEFORE" -> MoveLocation.BEFORE
            "AFTER"  -> MoveLocation.AFTER
            else     -> mcpFail("Invalid location '$location': must be INTO, BEFORE, or AFTER")
        }

        val tree = service.getBookmarksTree()
        bookmarkIds.forEach { id -> tree.getBookmark(id) ?: mcpFail("Bookmark not found: $id") }
        val target = tree.getBookmark(BookmarkId(targetId)) ?: mcpFail("Target not found: $targetId")
        if (loc == MoveLocation.INTO && target !is BookmarkFolder) mcpFail("'$targetId' is not a folder")

        return try {
            service.moveBookmarks(bookmarkIds, BookmarkId(targetId), loc)
            "Moved ${bookmarkIds.size} bookmark(s)"
        } catch (e: BookmarksException) {
            mcpFail("Could not move bookmarks: ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "Delete a bookmark (favori) or bookmark folder by its ID. IMPORTANT: When recursive=true, always ask the user for confirmation before calling this tool.")
    suspend fun delete_bookmark(
        @McpDescription(description = "The bookmark ID to delete") id: String,
        @McpDescription(description = "Whether to delete bookmark folder contents recursively (default: false)") recursive: Boolean = false
    ): String {
        val service = bookmarksService()
        service.getBookmarksTree().getBookmark(BookmarkId(id)) ?: mcpFail("Bookmark not found: $id")
        return try {
            service.deleteBookmarks(listOf(BookmarkId(id)), recursive)
            "Deleted bookmark: $id"
        } catch (e: BookmarksException) {
            mcpFail("Could not delete bookmark '$id': ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "Modify a bookmark (favori) by merging the given properties into its existing ones. Only specified properties are updated; others remain unchanged.")
    suspend fun modify_bookmark(
        @McpDescription(description = "The bookmark ID to modify") id: String,
        @McpDescription(description = "Properties to update. Use list_bookmark_properties to get the list of possible properties.") properties: Map<String, String>
    ): BookmarkResult {
        val service = bookmarksService()
        service.getBookmarksTree().getBookmark(BookmarkId(id)) ?: mcpFail("Bookmark not found: $id")
        return try {
            service.modifyBookmark(BookmarkId(id), properties)
            val updatedTree = service.getBookmarksTree()
            val bookmark = updatedTree.getBookmark(BookmarkId(id)) ?: mcpFail("Bookmark not found after modify: $id")
            bookmarkToResult(updatedTree, bookmark, propertyDescriptors(), null)
        } catch (e: BookmarksException) {
            mcpFail("Could not modify bookmark '$id': ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "Update a bookmark (favori) to a new file location, re-capturing file path, line number and line content.")
    suspend fun update_bookmark(
        @McpDescription(description = "The bookmark ID to update") id: String,
        @McpDescription(description = "Absolute path to the file") filePath: String,
        @McpDescription(description = "Line number (1-based)") lineNumber: Int,
        @McpDescription(description = "New comment (leave blank to keep existing)") comment: String = ""
    ): BookmarkResult {
        val project = currentProject()
        val service = bookmarksService()

        service.getBookmarksTree().getBookmark(BookmarkId(id)) ?: mcpFail("Bookmark not found: $id")
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath)
            ?: mcpFail("File not found: $filePath")

        var error: String? = null
        ApplicationManager.getApplication().invokeAndWait {
            val document = FileDocumentManager.getInstance().getDocument(virtualFile)
                ?: run { error = "Cannot read document for: $filePath"; return@invokeAndWait }
            if (lineNumber < 1 || lineNumber > document.lineCount) {
                error = "Line number $lineNumber is out of range (1..${document.lineCount})"
                return@invokeAndWait
            }
            val editor = EditorFactory.getInstance().createEditor(document, project, virtualFile.fileType, true)
            try {
                editor.caretModel.moveToOffset(document.getLineStartOffset(lineNumber - 1))
                val dataContext = SimpleDataContext.builder()
                    .add(CommonDataKeys.EDITOR, editor)
                    .add(CommonDataKeys.VIRTUAL_FILE, virtualFile)
                    .add(CommonDataKeys.PROJECT, project)
                    .build()
                service.updateBookmark(BookmarkId(id), dataContext, EmptyProgressIndicator())
            } catch (e: BookmarksException) {
                error = "Could not update bookmark: ${e.message}"
            } finally {
                EditorFactory.getInstance().releaseEditor(editor)
            }
        }

        error?.let { mcpFail(it) }
        if (comment.isNotBlank()) {
            try {
                service.modifyBookmark(BookmarkId(id), mapOf(Bookmark.PROPERTY_COMMENT to comment))
            } catch (e: BookmarksException) {
                mcpFail("Bookmark updated but could not set comment: ${e.message}")
            }
        }
        val updatedTree = service.getBookmarksTree()
        val bookmark = updatedTree.getBookmark(BookmarkId(id)) ?: mcpFail("Bookmark not found after update: $id")
        return bookmarkToResult(updatedTree, bookmark, propertyDescriptors(), null)
    }

    @McpTool
    @McpDescription(description = "Add a bookmark (favori) for a file at a specific line.")
    suspend fun add_file_bookmark(
        @McpDescription(description = "Absolute path to the file") filePath: String,
        @McpDescription(description = "Line number (1-based). Read the file first to find the exact line of the element you want to bookmark — do not default to 1.") lineNumber: Int,
        @McpDescription(description = "Optional comment for the bookmark. Only the first line is shown in the bookmark tree; the full multi-line comment is visible in the details panel.") comment: String = "",
        @McpDescription(description = "ID of the parent bookmark folder (default: currently selected folder, then 'default' folder, then root). The 'default' folder is a general-purpose inbox for temporary or yet-to-be-organized bookmarks.") parentId: String = ""
    ): BookmarkResult {
        val project = currentProject()
        val service = bookmarksService()

        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath)
            ?: mcpFail("File not found: $filePath")

        val resolvedParentFolderId: BookmarkId? = if (parentId.isNotBlank()) {
            val parent = service.getBookmarksTree().getBookmark(BookmarkId(parentId)) ?: mcpFail("Parent folder not found: $parentId")
            if (parent !is BookmarkFolder) mcpFail("'$parentId' is not a folder")
            BookmarkId(parentId)
        } else null
        var newId: BookmarkId? = null
        var error: String? = null
        ApplicationManager.getApplication().invokeAndWait {
            val document = FileDocumentManager.getInstance().getDocument(virtualFile)
                ?: run { error = "Cannot read document for: $filePath"; return@invokeAndWait }
            if (lineNumber < 1 || lineNumber > document.lineCount) {
                error = "Line number $lineNumber is out of range (1..${document.lineCount})"
                return@invokeAndWait
            }
            val editor = EditorFactory.getInstance().createEditor(document, project, virtualFile.fileType, true)
            try {
                editor.caretModel.moveToOffset(document.getLineStartOffset(lineNumber - 1))
                val dataContext = SimpleDataContext.builder()
                    .add(CommonDataKeys.EDITOR, editor)
                    .add(CommonDataKeys.VIRTUAL_FILE, virtualFile)
                    .add(CommonDataKeys.PROJECT, project)
                    .build()
                newId = service.addBookmark(dataContext, resolvedParentFolderId, EmptyProgressIndicator())
            } catch (e: BookmarksException) {
                error = "Could not add bookmark: ${e.message}"
            } finally {
                EditorFactory.getInstance().releaseEditor(editor)
            }
        }

        error?.let { mcpFail(it) }
        val id = newId ?: mcpFail("Could not create bookmark")
        try {
            val existingProps = service.getBookmarksTree().getBookmark(id)?.properties ?: emptyMap()
            val extraProps = mutableMapOf(McpBookmarkProperties.PROPERTY_ORIGIN to McpBookmarkProperties.ORIGIN_MCP)
            if (comment.isNotBlank()) extraProps[Bookmark.PROPERTY_COMMENT] = comment
            service.setBookmarkProperties(id, existingProps + extraProps)
        } catch (e: BookmarksException) {
            mcpFail("Bookmark created but could not set properties: ${e.message}")
        }
        val updatedTree = service.getBookmarksTree()
        val bookmark = updatedTree.getBookmark(id) ?: mcpFail("Bookmark was created but could not be retrieved")
        return bookmarkToResult(updatedTree, bookmark, propertyDescriptors(), null)
    }

    private fun getSelectedFolderIdFromToolWindow(project: Project): BookmarkId? {
        var result: BookmarkId? = null
        ApplicationManager.getApplication().invokeAndWait {
            val treeComponent = MesFavorisToolWindowUtils.findBookmarksTree(project) ?: return@invokeAndWait
            val selectionPath = treeComponent.selectionModel.selectionPath ?: return@invokeAndWait
            val bookmark = treeComponent.getBookmark(selectionPath) ?: return@invokeAndWait
            if (bookmark is BookmarkFolder) result = bookmark.id
        }
        return result
    }

    @Serializable
    enum class PropertyType {
        @SerialName("string")  STRING,
        @SerialName("path")    PATH,
        @SerialName("int")     INT,
        @SerialName("instant") INSTANT
    }

    @Serializable
    data class PropertyDescriptorResult(
        @property:McpDescription("Property name (e.g. 'filePath', 'lineNumber')")
        val name: String,
        @property:McpDescription("Value type: string (text), path (absolute file system path), int (integer), instant (timestamp)")
        val type: PropertyType,
        @property:McpDescription("Whether this property can be updated after creation")
        val updatable: Boolean,
        @property:McpDescription("Whether this property is omitted from bookmark results by default (e.g. large base64 blobs). Request it explicitly via 'returnProperties' to include it.")
        val excludedFromMcp: Boolean,
        @property:McpDescription("Human-readable description of the property")
        val description: String? = null
    )

    @Serializable
    data class PropertyDescriptorsResult(
        @property:McpDescription("The list of property descriptors")
        val descriptors: List<PropertyDescriptorResult>
    )
}
