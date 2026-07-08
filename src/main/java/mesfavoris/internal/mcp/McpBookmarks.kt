package mesfavoris.internal.mcp

import com.intellij.mcpserver.annotations.McpDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mesfavoris.bookmarktype.IBookmarkPropertyDescriptors
import mesfavoris.internal.bookmarktypes.extension.ExtensionBookmarkPropertyDescriptors
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
    val bookmarks: List<BookmarkResult>,
    @property:McpDescription("Opaque cursor to pass back to fetch the next page; null when there are no more results")
    val nextCursor: String? = null,
    @property:McpDescription("Total number of matching entries available (across all pages); null when the call is not paginated")
    val total: Int? = null
)

/**
 * Converts a bookmark to its MCP result form, projecting its properties.
 *
 * @param requested when non-null, only these property keys are returned (explicitly requested keys are
 *   included even if their descriptor is marked excluded); when null, all properties are returned except
 *   those whose descriptor is marked [BookmarkPropertyDescriptor.isExcludedFromMcp].
 */
fun bookmarkToResult(
    tree: BookmarksTree,
    bookmark: Bookmark,
    descriptors: IBookmarkPropertyDescriptors = ExtensionBookmarkPropertyDescriptors(),
    requested: Set<String>? = null
): BookmarkResult {
    val resolver = PathPlaceholderResolver(PathPlaceholdersStore.getInstance())
    val properties = bookmark.properties
        .filterKeys { key ->
            if (requested != null) key in requested
            else descriptors.getPropertyDescriptor(key)?.isExcludedFromMcp != true
        }
        .mapValues { (k, v) ->
            if (k == PROP_FILE_PATH) resolver.expand(v)?.toString() ?: v else v
        }
    return BookmarkResult(
        id = bookmark.id.toString(),
        type = if (bookmark is BookmarkFolder) BookmarkType.FOLDER else BookmarkType.BOOKMARK,
        properties = properties,
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
