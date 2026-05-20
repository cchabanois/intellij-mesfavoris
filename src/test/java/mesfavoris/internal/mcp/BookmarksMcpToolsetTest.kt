package mesfavoris.internal.mcp

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import kotlinx.coroutines.runBlocking
import mesfavoris.model.BookmarkDatabase
import mesfavoris.model.BookmarkId
import mesfavoris.path.PathBookmarkProperties.PROP_FILE_PATH
import mesfavoris.service.IBookmarksService
import mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark
import mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.nio.file.Paths

class BookmarksMcpToolsetTest : BasePlatformTestCase() {

    private lateinit var toolset: BookmarksMcpToolset
    private lateinit var bookmarkDatabase: BookmarkDatabase
    private lateinit var rootFolderId: BookmarkId
    private lateinit var workFolderId: BookmarkId
    private lateinit var projectsFolderId: BookmarkId
    private lateinit var personalFolderId: BookmarkId
    private lateinit var taskBookmarkId: BookmarkId
    private lateinit var projectBookmarkId: BookmarkId
    private lateinit var blogBookmarkId: BookmarkId

    override fun createTempDirTestFixture(): TempDirTestFixture = TempDirTestFixtureImpl()

    override fun getTestDataPath() = "src/test/testData"

    @Before
    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("commons-cli", "commons-cli")
        toolset = BookmarksMcpToolset()
        val service = project.getService(IBookmarksService::class.java)
        bookmarkDatabase = service.getBookmarkDatabase()
        rootFolderId = bookmarkDatabase.getBookmarksTree().rootFolder.id

        workFolderId = BookmarkId()
        projectsFolderId = BookmarkId()
        personalFolderId = BookmarkId()
        taskBookmarkId = BookmarkId()
        projectBookmarkId = BookmarkId()
        blogBookmarkId = BookmarkId()

        // root
        // ├── work (folder)
        // │   ├── projects (folder)
        // │   │   └── "My Project" bookmark
        // │   └── "Daily Task" bookmark (comment: "important work")
        // └── personal (folder)
        //     └── "My Blog" bookmark
        val existing = bookmarkDatabase.getBookmarksTree().getChildren(rootFolderId)
        if (existing.isNotEmpty()) {
            bookmarkDatabase.modify { modifier ->
                existing.forEach { modifier.deleteBookmark(it.id, true) }
            }
        }

        bookmarkDatabase.modify { modifier ->
            modifier.addBookmarks(rootFolderId, listOf(
                bookmarkFolder(workFolderId, "work").build(),
                bookmarkFolder(personalFolderId, "personal").build()
            ))
            modifier.addBookmarks(workFolderId, listOf(
                bookmarkFolder(projectsFolderId, "projects").build(),
                bookmark(taskBookmarkId, "Daily Task").withProperty("comment", "important work").build()
            ))
            modifier.addBookmarks(projectsFolderId, listOf(
                bookmark(projectBookmarkId, "My Project").build()
            ))
            modifier.addBookmarks(personalFolderId, listOf(
                bookmark(blogBookmarkId, "My Blog").build()
            ))
        }
    }

    // --- search_bookmarks ---

    @Test
    fun testSearchReturnsAllMatchingBookmarksAcrossTree() {
        runBlocking {
            val results = toolset.search_bookmarks(query = "my").bookmarks

            assertThat(results.size).isEqualTo(2)
            assertThat(results.ids()).containsExactlyInAnyOrder(
                projectBookmarkId.toString(), blogBookmarkId.toString()
            )
        }
    }

    @Test
    fun testSearchIsCaseInsensitive() {
        runBlocking {
            val results = toolset.search_bookmarks(query = "DAILY").bookmarks

            assertThat(results.size).isEqualTo(1)
            assertThat(results.ids()).containsExactly(taskBookmarkId.toString())
        }
    }

    @Test
    fun testSearchWithParentIdLimitsToSubtree() {
        runBlocking {
            val results = toolset.search_bookmarks(parentId = workFolderId.toString(), query = "my").bookmarks

            assertThat(results.size).isEqualTo(1)
            assertThat(results.ids()).containsExactly(projectBookmarkId.toString())
        }
    }

    @Test
    fun testSearchNonRecursiveReturnsOnlyDirectChildren() {
        runBlocking {
            val results = toolset.search_bookmarks(parentId = workFolderId.toString(), query = "task", recursive = false).bookmarks

            assertThat(results.size).isEqualTo(1)
            assertThat(results.ids()).containsExactly(taskBookmarkId.toString())
        }
    }

    @Test
    fun testSearchNonRecursiveDoesNotReturnNestedBookmarks() {
        runBlocking {
            val results = toolset.search_bookmarks(parentId = workFolderId.toString(), query = "project", recursive = false).bookmarks

            // "projects" folder is a direct child but "My Project" bookmark is not
            assertThat(results.size).isEqualTo(1)
            assertThat(results.ids()).containsExactly(projectsFolderId.toString())
        }
    }

    @Test
    fun testSearchWithAttributesFiltersOnSpecificProperties() {
        runBlocking {
            val results = toolset.search_bookmarks(query = "important", attributes = "comment").bookmarks

            assertThat(results.size).isEqualTo(1)
            assertThat(results.ids()).containsExactly(taskBookmarkId.toString())
        }
    }

    @Test
    fun testSearchWithAttributesDoesNotMatchOtherProperties() {
        runBlocking {
            val results = toolset.search_bookmarks(query = "Daily", attributes = "comment").bookmarks

            assertThat(results.size).isEqualTo(0)
        }
    }

    @Test
    fun testSearchRespectsMaxResults() {
        runBlocking {
            val results = toolset.search_bookmarks(query = "a", maxResults = 2).bookmarks

            assertThat(results.size).isLessThanOrEqualTo(2)
        }
    }

    @Test
    fun testSearchResultHasCorrectFolderPath() {
        runBlocking {
            val results = toolset.search_bookmarks(query = "My Project").bookmarks

            assertThat(results.size).isEqualTo(1)
            assertThat(results[0].folderPath).isEqualTo("/work/projects")
        }
    }

    @Test
    fun testSearchResultTypeIsBookmarkForNonFolder() {
        runBlocking {
            val results = toolset.search_bookmarks(query = "Daily Task").bookmarks

            assertThat(results.size).isEqualTo(1)
            assertThat(results[0].type).isEqualTo(BookmarksMcpToolset.BookmarkType.BOOKMARK)
        }
    }

    @Test
    fun testSearchResultTypeIsFolderForBookmarkFolder() {
        runBlocking {
            val results = toolset.search_bookmarks(query = "projects").bookmarks

            val folderResult = results.firstOrNull { it.id == projectsFolderId.toString() }
            assertThat(folderResult).isNotNull
            assertThat(folderResult!!.type).isEqualTo(BookmarksMcpToolset.BookmarkType.FOLDER)
        }
    }

    @Test
    fun testSearchWithInvalidParentIdFails() {
        assertMcpFails("not found") {
            runBlocking { toolset.search_bookmarks(parentId = "nonexistent", query = "test") }
        }
    }

    @Test
    fun testSearchBookmarkFilePathPlaceholderIsExpanded() {
        val homeDir = Paths.get(System.getProperty("user.home"))
        val bookmarkWithPathId = BookmarkId()
        bookmarkDatabase.modify { modifier ->
            modifier.addBookmarks(rootFolderId, listOf(
                bookmark(bookmarkWithPathId, "Path Bookmark")
                    .withProperty(PROP_FILE_PATH, "\${HOME}/test.txt")
                    .build()
            ))
        }

        runBlocking {
            val results = toolset.search_bookmarks(query = "Path Bookmark").bookmarks

            assertThat(results.size).isEqualTo(1)
            assertThat(results[0].properties[PROP_FILE_PATH]).isEqualTo(homeDir.resolve("test.txt").toString())
        }
    }

    // --- create_bookmark_folder ---

    @Test
    fun testCreateFolderWithExplicitParentId() {
        runBlocking {
            val result = toolset.create_bookmark_folder(name = "New Folder", parentId = workFolderId.toString())

            assertThat(result.type).isEqualTo(BookmarksMcpToolset.BookmarkType.FOLDER)
            assertThat(result.properties["name"]).isEqualTo("New Folder")
            assertThat(result.folderPath).isEqualTo("/work")
            assertThat(result.id).isNotBlank()
        }
    }

    @Test
    fun testCreateFolderIsPersistedInTree() {
        runBlocking {
            val result = toolset.create_bookmark_folder(name = "Persisted", parentId = workFolderId.toString())
            val newId = BookmarkId(result.id)

            val tree = bookmarkDatabase.getBookmarksTree()
            assertThat(tree.getBookmark(newId)).isNotNull
            assertThat(tree.getParentBookmark(newId)?.id).isEqualTo(workFolderId)
        }
    }

    @Test
    fun testCreateFolderWithoutParentIdUsesDefaultFolderIfExists() {
        runBlocking {
            val result = toolset.create_bookmark_folder(name = "Root Level Folder")

            assertThat(result.folderPath).isIn("/", "/default")
        }
    }

    @Test
    fun testCreateFolderWithBlankNameFails() {
        assertMcpFails("blank") {
            runBlocking { toolset.create_bookmark_folder(name = "  ") }
        }
    }

    @Test
    fun testCreateFolderWithInvalidParentIdFails() {
        assertMcpFails("not found") {
            runBlocking { toolset.create_bookmark_folder(name = "Test", parentId = "nonexistent") }
        }
    }

    @Test
    fun testCreateFolderWithNonFolderParentIdFails() {
        assertMcpFails("not a folder") {
            runBlocking { toolset.create_bookmark_folder(name = "Test", parentId = taskBookmarkId.toString()) }
        }
    }

    // --- add_bookmark ---

    @Test
    fun testAddBookmarkCreatesBookmarkForFile() {
        runBlocking {
            val result = toolset.add_bookmark(filePath = testFilePath(), lineNumber = 2)

            assertThat(result.type).isEqualTo(BookmarksMcpToolset.BookmarkType.BOOKMARK)
            assertThat(result.id).isNotBlank()
        }
    }

    @Test
    fun testAddBookmarkIsPersistedInTree() {
        runBlocking {
            val result = toolset.add_bookmark(filePath = testFilePath(), lineNumber = 1)
            val id = BookmarkId(result.id)

            assertThat(bookmarkDatabase.getBookmarksTree().getBookmark(id)).isNotNull
        }
    }

    @Test
    fun testAddBookmarkWithCommentSetsCommentProperty() {
        runBlocking {
            val result = toolset.add_bookmark(filePath = testFilePath(), lineNumber = 1, comment = "my comment")
            val id = BookmarkId(result.id)

            val bookmark = bookmarkDatabase.getBookmarksTree().getBookmark(id)
            assertThat(bookmark?.getPropertyValue("comment")).isEqualTo("my comment")
        }
    }

    @Test
    fun testAddBookmarkWithCommentPreservesOtherProperties() {
        runBlocking {
            val result = toolset.add_bookmark(filePath = testFilePath(), lineNumber = 1, comment = "my comment")
            val id = BookmarkId(result.id)

            val bookmark = bookmarkDatabase.getBookmarksTree().getBookmark(id)
            assertThat(bookmark?.properties).containsKey("name")
            assertThat(bookmark?.getPropertyValue("comment")).isEqualTo("my comment")
        }
    }

    @Test
    fun testAddBookmarkWithParentIdPlacesBookmarkInFolder() {
        runBlocking {
            val result = toolset.add_bookmark(filePath = testFilePath(), lineNumber = 1, parentId = workFolderId.toString())
            val id = BookmarkId(result.id)

            assertThat(bookmarkDatabase.getBookmarksTree().getParentBookmark(id)?.id).isEqualTo(workFolderId)
        }
    }

    @Test
    fun testAddBookmarkFolderPathReflectsParentId() {
        runBlocking {
            val result = toolset.add_bookmark(filePath = testFilePath(), lineNumber = 1, parentId = projectsFolderId.toString())

            assertThat(result.folderPath).isEqualTo("/work/projects")
        }
    }

    @Test
    fun testAddBookmarkWithInvalidFilePathFails() {
        assertMcpFails("not found") {
            runBlocking { toolset.add_bookmark(filePath = "/nonexistent/path/file.java", lineNumber = 1) }
        }
    }

    @Test
    fun testAddBookmarkWithLineNumberOutOfRangeFails() {
        assertMcpFails("out of range") {
            runBlocking { toolset.add_bookmark(filePath = testFilePath(), lineNumber = 10000) }
        }
    }

    @Test
    fun testAddBookmarkWithZeroLineNumberFails() {
        assertMcpFails("out of range") {
            runBlocking { toolset.add_bookmark(filePath = testFilePath(), lineNumber = 0) }
        }
    }

    @Test
    fun testAddBookmarkWithInvalidParentIdFails() {
        assertMcpFails("not found") {
            runBlocking { toolset.add_bookmark(filePath = testFilePath(), lineNumber = 1, parentId = "nonexistent") }
        }
    }

    @Test
    fun testAddBookmarkWithNonFolderParentIdFails() {
        assertMcpFails("not a folder") {
            runBlocking { toolset.add_bookmark(filePath = testFilePath(), lineNumber = 1, parentId = taskBookmarkId.toString()) }
        }
    }

    // --- list_bookmark_folder ---

    @Test
    fun testListBookmarkFolderReturnsDirectChildren() {
        runBlocking {
            val results = toolset.list_bookmark_folder(folderId = workFolderId.toString()).bookmarks

            assertThat(results.ids()).containsExactlyInAnyOrder(
                projectsFolderId.toString(), taskBookmarkId.toString()
            )
        }
    }

    @Test
    fun testListBookmarkFolderDefaultsToRootFolder() {
        runBlocking {
            val results = toolset.list_bookmark_folder().bookmarks

            assertThat(results.ids()).containsExactlyInAnyOrder(
                workFolderId.toString(), personalFolderId.toString()
            )
        }
    }

    @Test
    fun testListBookmarkFolderNonRecursiveDoesNotReturnDescendants() {
        runBlocking {
            val results = toolset.list_bookmark_folder(folderId = workFolderId.toString(), recursive = false).bookmarks

            assertThat(results.ids()).doesNotContain(projectBookmarkId.toString())
        }
    }

    @Test
    fun testListBookmarkFolderRecursiveReturnsAllDescendants() {
        runBlocking {
            val results = toolset.list_bookmark_folder(folderId = workFolderId.toString(), recursive = true).bookmarks

            assertThat(results.ids()).containsExactlyInAnyOrder(
                projectsFolderId.toString(), taskBookmarkId.toString(), projectBookmarkId.toString()
            )
        }
    }

    @Test
    fun testListBookmarkFolderWithInvalidIdFails() {
        assertMcpFails("not found") {
            runBlocking { toolset.list_bookmark_folder(folderId = "nonexistent") }
        }
    }

    @Test
    fun testListBookmarkFolderWithNonFolderIdFails() {
        assertMcpFails("not a folder") {
            runBlocking { toolset.list_bookmark_folder(folderId = taskBookmarkId.toString()) }
        }
    }

    // --- move_bookmarks ---

    @Test
    fun testMoveBookmarksIntoFolder() {
        runBlocking {
            toolset.move_bookmarks(ids = taskBookmarkId.toString(), targetId = personalFolderId.toString(), location = "INTO")

            assertThat(bookmarkDatabase.getBookmarksTree().getParentBookmark(taskBookmarkId)?.id).isEqualTo(personalFolderId)
        }
    }

    @Test
    fun testMoveBookmarksBeforeTarget() {
        runBlocking {
            // Move projectBookmarkId before taskBookmarkId (both end up in workFolderId)
            toolset.move_bookmarks(ids = projectBookmarkId.toString(), targetId = taskBookmarkId.toString(), location = "BEFORE")

            val children = bookmarkDatabase.getBookmarksTree().getChildren(workFolderId).map { it.id }
            assertThat(children.indexOf(projectBookmarkId)).isLessThan(children.indexOf(taskBookmarkId))
        }
    }

    @Test
    fun testMoveBookmarksAfterTarget() {
        runBlocking {
            // Move projectBookmarkId after taskBookmarkId (both end up in workFolderId)
            toolset.move_bookmarks(ids = projectBookmarkId.toString(), targetId = taskBookmarkId.toString(), location = "AFTER")

            val children = bookmarkDatabase.getBookmarksTree().getChildren(workFolderId).map { it.id }
            assertThat(children.indexOf(projectBookmarkId)).isGreaterThan(children.indexOf(taskBookmarkId))
        }
    }

    @Test
    fun testMoveMultipleBookmarks() {
        runBlocking {
            toolset.move_bookmarks(
                ids = "${taskBookmarkId},${projectBookmarkId}",
                targetId = personalFolderId.toString(),
                location = "INTO"
            )

            val tree = bookmarkDatabase.getBookmarksTree()
            assertThat(tree.getParentBookmark(taskBookmarkId)?.id).isEqualTo(personalFolderId)
            assertThat(tree.getParentBookmark(projectBookmarkId)?.id).isEqualTo(personalFolderId)
        }
    }

    @Test
    fun testMoveBookmarksWithInvalidIdFails() {
        assertMcpFails("not found") {
            runBlocking {
                toolset.move_bookmarks(ids = "nonexistent", targetId = personalFolderId.toString(), location = "INTO")
            }
        }
    }

    @Test
    fun testMoveBookmarksWithInvalidTargetFails() {
        assertMcpFails("not found") {
            runBlocking {
                toolset.move_bookmarks(ids = taskBookmarkId.toString(), targetId = "nonexistent", location = "INTO")
            }
        }
    }

    @Test
    fun testMoveBookmarksIntoNonFolderFails() {
        assertMcpFails("not a folder") {
            runBlocking {
                toolset.move_bookmarks(ids = projectBookmarkId.toString(), targetId = taskBookmarkId.toString(), location = "INTO")
            }
        }
    }

    @Test
    fun testMoveBookmarksWithInvalidLocationFails() {
        assertMcpFails("invalid location") {
            runBlocking {
                toolset.move_bookmarks(ids = taskBookmarkId.toString(), targetId = personalFolderId.toString(), location = "SIDEWAYS")
            }
        }
    }

    // --- delete_bookmark ---

    @Test
    fun testDeleteBookmarkRemovesItFromTree() {
        runBlocking {
            toolset.delete_bookmark(id = taskBookmarkId.toString())

            assertThat(bookmarkDatabase.getBookmarksTree().getBookmark(taskBookmarkId)).isNull()
        }
    }

    @Test
    fun testDeleteFolderWithoutRecursiveFails() {
        assertMcpFails("") {
            runBlocking { toolset.delete_bookmark(id = workFolderId.toString(), recursive = false) }
        }
    }

    @Test
    fun testDeleteFolderRecursivelyRemovesFolderAndContents() {
        runBlocking {
            toolset.delete_bookmark(id = workFolderId.toString(), recursive = true)

            val tree = bookmarkDatabase.getBookmarksTree()
            assertThat(tree.getBookmark(workFolderId)).isNull()
            assertThat(tree.getBookmark(projectsFolderId)).isNull()
            assertThat(tree.getBookmark(taskBookmarkId)).isNull()
        }
    }

    @Test
    fun testDeleteWithInvalidIdFails() {
        assertMcpFails("not found") {
            runBlocking { toolset.delete_bookmark(id = "nonexistent") }
        }
    }

    // --- list_bookmark_properties ---

    @Test
    fun testListBookmarkPropertiesReturnsKnownProperties() {
        runBlocking {
            val descriptors = toolset.list_bookmark_properties().descriptors

            assertThat(descriptors.size).isGreaterThan(0)
            assertThat(descriptors.map { it.name }).contains("filePath", "lineNumber", "lineContent")
        }
    }

    @Test
    fun testListBookmarkPropertiesHasRequiredFields() {
        runBlocking {
            val first = toolset.list_bookmark_properties().descriptors[0]

            assertThat(first.name).isNotBlank()
            assertThat(first.type).isNotNull()
        }
    }

    @Test
    fun testListBookmarkPropertiesFilePathHasTypeString() {
        runBlocking {
            val descriptors = toolset.list_bookmark_properties().descriptors

            val filePathDescriptor = descriptors.firstOrNull { it.name == "filePath" }
            assertThat(filePathDescriptor).isNotNull
            assertThat(filePathDescriptor!!.type).isEqualTo(BookmarksMcpToolset.PropertyType.PATH)
        }
    }

    // --- helpers ---

    private fun testFilePath() = myFixture.findFileInTempDir("commons-cli/src/main/java/org/apache/commons/cli/CommandLine.java").path

    private fun List<BookmarksMcpToolset.BookmarkResult>.ids() = map { it.id }


    private fun assertMcpFails(messageContains: String, block: () -> Unit) {
        try {
            block()
            fail("Expected an exception to be thrown")
        } catch (e: Throwable) {
            val message = e.message ?: e.cause?.message ?: ""
            assertThat(message).containsIgnoringCase(messageContains)
        }
    }
}
