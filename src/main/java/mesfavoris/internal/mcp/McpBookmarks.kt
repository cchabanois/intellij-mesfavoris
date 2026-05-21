package mesfavoris.internal.mcp

import com.intellij.mcpserver.annotations.McpDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mesfavoris.internal.placeholders.PathPlaceholderResolver
import mesfavoris.internal.settings.placeholders.PathPlaceholdersStore
import mesfavoris.model.Bookmark
import mesfavoris.model.BookmarkFolder
import mesfavoris.model.BookmarkId
import mesfavoris.model.BookmarksTree
import mesfavoris.path.PathBookmarkProperties.PROP_FILE_PATH

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
    @property:McpDescription("Key-value properties of the bookmark (e.g. name, filePath, lineNumber, comment). Use list_bookmark_properties to get the full list of possible properties with their descriptions.")
    val properties: Map<String, String>,
    @property:McpDescription("Path of the containing bookmark folder from root, e.g. '/work/projects'")
    val folderPath: String
)

@Serializable
data class BookmarksResult(
    @property:McpDescription("The list of bookmarks (favoris)")
    val bookmarks: List<BookmarkResult>
)

fun bookmarkToResult(tree: BookmarksTree, bookmark: Bookmark): BookmarkResult {
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

fun buildFolderPath(tree: BookmarksTree, bookmarkId: BookmarkId): String {
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
