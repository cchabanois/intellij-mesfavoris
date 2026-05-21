package mesfavoris.snippets.internal.mcp

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import mesfavoris.internal.mcp.BookmarkType
import mesfavoris.model.BookmarkDatabase
import mesfavoris.model.BookmarkId
import mesfavoris.service.IBookmarksService
import mesfavoris.snippets.SnippetBookmarkProperties.PROP_SNIPPET_CONTENT
import mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class SnippetsMcpToolsetTest : BasePlatformTestCase() {

    private lateinit var toolset: SnippetsMcpToolset
    private lateinit var bookmarkDatabase: BookmarkDatabase
    private lateinit var rootFolderId: BookmarkId
    private lateinit var shellFolderId: BookmarkId

    @Before
    override fun setUp() {
        super.setUp()
        toolset = SnippetsMcpToolset()
        val service = project.getService(IBookmarksService::class.java)
        bookmarkDatabase = service.getBookmarkDatabase()
        rootFolderId = bookmarkDatabase.getBookmarksTree().rootFolder.id

        shellFolderId = BookmarkId()
        bookmarkDatabase.modify { modifier ->
            modifier.addBookmarks(rootFolderId, listOf(
                bookmarkFolder(shellFolderId, "shell").build()
            ))
        }
    }

    @Test
    fun testAddSnippetCreatesBookmarkWithContent() {
        runBlocking {
            val result = toolset.add_snippet(content = "echo hello world")

            assertThat(result.type).isEqualTo(BookmarkType.BOOKMARK)
            assertThat(result.properties[PROP_SNIPPET_CONTENT]).isEqualTo("echo hello world")
        }
    }

    @Test
    fun testAddSnippetDerivesNameFromFirstLine() {
        runBlocking {
            val result = toolset.add_snippet(content = "echo hello\necho world")

            assertThat(result.properties["name"]).isEqualTo("echo hello")
        }
    }

    @Test
    fun testAddSnippetUsesExplicitName() {
        runBlocking {
            val result = toolset.add_snippet(content = "git status", name = "Check git status")

            assertThat(result.properties["name"]).isEqualTo("Check git status")
        }
    }

    @Test
    fun testAddSnippetWithComment() {
        runBlocking {
            val result = toolset.add_snippet(content = "ls -la", comment = "list all files")

            assertThat(result.properties["comment"]).isEqualTo("list all files")
        }
    }

    @Test
    fun testAddSnippetInSpecificFolder() {
        runBlocking {
            val result = toolset.add_snippet(content = "df -h", parentId = shellFolderId.toString())

            assertThat(result.folderPath).isEqualTo("/shell")
            val bookmark = bookmarkDatabase.getBookmarksTree().getBookmark(BookmarkId(result.id))
            assertThat(bookmark).isNotNull
        }
    }

    @Test
    fun testAddSnippetWithBlankContentFails() {
        assertMcpFails("blank") {
            runBlocking { toolset.add_snippet(content = "   ") }
        }
    }

    @Test
    fun testAddSnippetWithInvalidParentIdFails() {
        assertMcpFails("not found") {
            runBlocking { toolset.add_snippet(content = "echo hi", parentId = "nonexistent") }
        }
    }

    @Test
    fun testAddSnippetWithNonFolderParentIdFails() {
        runBlocking {
            val bookmark = toolset.add_snippet(content = "echo hi")
            assertMcpFails("not a folder") {
                runBlocking { toolset.add_snippet(content = "echo world", parentId = bookmark.id) }
            }
        }
    }

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
