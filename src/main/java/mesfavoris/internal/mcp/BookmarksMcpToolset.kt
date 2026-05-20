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
import mesfavoris.internal.Constants.DEFAULT_BOOKMARKFOLDER_ID
import mesfavoris.bookmarktype.BookmarkPropertyDescriptor
import mesfavoris.internal.bookmarktypes.extension.ExtensionBookmarkPropertyDescriptors
import mesfavoris.internal.placeholders.PathPlaceholderResolver
import mesfavoris.internal.settings.placeholders.PathPlaceholdersStore
import mesfavoris.internal.toolwindow.MesFavorisToolWindowUtils
import mesfavoris.model.Bookmark
import mesfavoris.model.BookmarkFolder
import mesfavoris.model.BookmarkId
import mesfavoris.model.BookmarksTree
import mesfavoris.path.PathBookmarkProperties.PROP_FILE_PATH
import mesfavoris.service.IBookmarksService
import mesfavoris.service.MoveLocation

class BookmarksMcpToolset : McpToolset {

    private suspend fun currentProject(): Project =
        runCatching { currentCoroutineContext().projectOrNull }.getOrNull()
            ?: ProjectManager.getInstance().openProjects.firstOrNull()
            ?: mcpFail("No active project")

    private suspend fun bookmarksService(): IBookmarksService =
        currentProject().getService(IBookmarksService::class.java)
            ?: mcpFail("Bookmarks service unavailable")

    @McpTool
    @McpDescription(description = "List all known bookmark property descriptors. Returns a JSON array describing the possible properties of a bookmark.")
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
                description = descriptor.description
            )
        })
    }

    @McpTool
    @McpDescription(description = "Navigate to a bookmark in the IDE by its ID")
    suspend fun goto_bookmark(
        @McpDescription(description = "The bookmark ID to navigate to") id: String
    ): String {
        val service = bookmarksService()
        return try {
            service.gotoBookmark(BookmarkId(id), EmptyProgressIndicator())
            "Navigated to bookmark: $id"
        } catch (e: BookmarksException) {
            mcpFail("Could not navigate to bookmark '$id': ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "Search bookmarks by text. Returns matching bookmarks as JSON.")
    suspend fun search_bookmarks(
        @McpDescription(description = "ID of the folder to search in (default: entire tree)") parentId: String = "",
        @McpDescription(description = "Text to search for (case-insensitive)") query: String,
        @McpDescription(description = "Whether to search recursively in subfolders (default: true)") recursive: Boolean = true,
        @McpDescription(description = "Comma-separated list of property names to search in (default: all properties)") attributes: String = "",
        @McpDescription(description = "Maximum number of results to return (default: 100)") maxResults: Int = 100
    ): BookmarksResult {
        val service = bookmarksService()
        val tree = service.getBookmarksTree()
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
            results.add(bookmarkToResult(tree, bookmark))
        }
        return BookmarksResult(results)
    }

    @McpTool
    @McpDescription(description = "Create a bookmark folder. Returns the created folder as JSON.")
    suspend fun create_bookmark_folder(
        @McpDescription(description = "Name of the folder to create (must not be blank)") name: String,
        @McpDescription(description = "ID of the parent folder (default: currently selected folder, then 'default' folder, then root)") parentId: String = ""
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
            val updatedTree = service.getBookmarksTree()
            val folder = updatedTree.getBookmark(newId) ?: mcpFail("Folder was created but could not be retrieved")
            bookmarkToResult(updatedTree, folder)
        } catch (e: BookmarksException) {
            mcpFail("Could not create folder '$name': ${e.message}")
        }
    }

    @McpTool
    @McpDescription(description = "List bookmarks in a folder. Returns a JSON array of direct children, or all descendants if recursive.")
    suspend fun list_bookmark_folder(
        @McpDescription(description = "ID of the folder to list (default: root folder)") folderId: String = "",
        @McpDescription(description = "Whether to list recursively (default: false)") recursive: Boolean = false
    ): BookmarksResult {
        val service = bookmarksService()
        val tree = service.getBookmarksTree()
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
            .map { bookmarkToResult(tree, it) })
    }

    @McpTool
    @McpDescription(description = "Move bookmarks to a new location. Returns a confirmation message.")
    suspend fun move_bookmarks(
        @McpDescription(description = "Comma-separated list of bookmark IDs to move") ids: String,
        @McpDescription(description = "Target bookmark ID") targetId: String,
        @McpDescription(description = "Where to place the bookmarks: INTO (into a folder), BEFORE or AFTER (relative to target)") location: String = "INTO"
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
    @McpDescription(description = "Delete a bookmark or folder by its ID.")
    suspend fun delete_bookmark(
        @McpDescription(description = "The bookmark ID to delete") id: String,
        @McpDescription(description = "Whether to delete folder contents recursively (default: false)") recursive: Boolean = false
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
    @McpDescription(description = "Add a bookmark for a file at a specific line. Returns the created bookmark as JSON.")
    suspend fun add_bookmark(
        @McpDescription(description = "Absolute path to the file") filePath: String,
        @McpDescription(description = "Line number (1-based)") lineNumber: Int,
        @McpDescription(description = "Optional comment for the bookmark") comment: String = "",
        @McpDescription(description = "ID of the parent folder (default: currently selected folder, then 'default' folder, then root)") parentId: String = ""
    ): BookmarkResult {
        val project = currentProject()
        val service = bookmarksService()

        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath)
            ?: mcpFail("File not found: $filePath")

        val tree = service.getBookmarksTree()
        val resolvedParentFolderId: BookmarkId = if (parentId.isNotBlank()) {
            val parent = tree.getBookmark(BookmarkId(parentId)) ?: mcpFail("Parent folder not found: $parentId")
            if (parent !is BookmarkFolder) mcpFail("'$parentId' is not a folder")
            BookmarkId(parentId)
        } else {
            getSelectedFolderIdFromToolWindow(project)
                ?: if (tree.getBookmark(DEFAULT_BOOKMARKFOLDER_ID) != null) DEFAULT_BOOKMARKFOLDER_ID
                   else tree.rootFolder.id
        }
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
        if (comment.isNotBlank()) {
            try {
                val existingProps = service.getBookmarksTree().getBookmark(id)?.properties ?: emptyMap()
                service.setBookmarkProperties(id, existingProps + (Bookmark.PROPERTY_COMMENT to comment))
            } catch (e: BookmarksException) {
                mcpFail("Bookmark created but could not set comment: ${e.message}")
            }
        }
        val updatedTree = service.getBookmarksTree()
        val bookmark = updatedTree.getBookmark(id) ?: mcpFail("Bookmark was created but could not be retrieved")
        return bookmarkToResult(updatedTree, bookmark)
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

    private fun bookmarkToResult(tree: BookmarksTree, bookmark: Bookmark): BookmarkResult {
        val resolver = PathPlaceholderResolver(PathPlaceholdersStore.getInstance())
        val expandedProperties = bookmark.properties.mapValues { (k, v) ->
            if (k == PROP_FILE_PATH) resolver.expand(v)?.toString() ?: v else v
        }
        return BookmarkResult(
            id = bookmark.id.toString(),
            type = if (bookmark is BookmarkFolder) BookmarkType.FOLDER else BookmarkType.BOOKMARK,
            properties = expandedProperties,
            folderPath = buildFolderPath(tree, bookmark.id)
        )
    }

    private fun buildFolderPath(tree: BookmarksTree, bookmarkId: BookmarkId): String {
        val segments = mutableListOf<String>()
        val rootId = tree.rootFolder.id
        var current = tree.getParentBookmark(bookmarkId)
        while (current != null && current.id != rootId) {
            val name = current.getPropertyValue(Bookmark.PROPERTY_NAME) ?: ""
            segments.add(name.replace("/", "\\/"))
            current = tree.getParentBookmark(current.id)
        }
        segments.reverse()
        return "/" + segments.joinToString("/")
    }

    @Serializable
    enum class BookmarkType {
        @SerialName("folder")   FOLDER,
        @SerialName("bookmark") BOOKMARK
    }

    @Serializable
    data class BookmarkResult(
        @property:McpDescription("Unique identifier of the bookmark")
        val id: String,
        @property:McpDescription("Type of the entry: folder or bookmark")
        val type: BookmarkType,
        @property:McpDescription("Key-value properties of the bookmark (e.g. name, filePath, lineNumber, comment)")
        val properties: Map<String, String>,
        @property:McpDescription("Path of the containing bookmark folder from root, e.g. '/work/projects'")
        val folderPath: String
    )

    @Serializable
    data class BookmarksResult(
        @property:McpDescription("The list of bookmarks")
        val bookmarks: List<BookmarkResult>
    )

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
        @property:McpDescription("Human-readable description of the property")
        val description: String? = null
    )

    @Serializable
    data class PropertyDescriptorsResult(
        @property:McpDescription("The list of property descriptors")
        val descriptors: List<PropertyDescriptorResult>
    )
}
