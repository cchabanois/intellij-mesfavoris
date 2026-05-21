package mesfavoris.url.internal.mcp

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import mesfavoris.internal.mcp.BookmarkType
import mesfavoris.model.BookmarkDatabase
import mesfavoris.model.BookmarkId
import mesfavoris.service.IBookmarksService
import mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder
import mesfavoris.url.UrlBookmarkProperties.PROP_URL
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class UrlsMcpToolsetTest : BasePlatformTestCase() {

    private lateinit var toolset: UrlsMcpToolset
    private lateinit var bookmarkDatabase: BookmarkDatabase
    private lateinit var rootFolderId: BookmarkId
    private lateinit var linksFolderId: BookmarkId

    @Before
    override fun setUp() {
        super.setUp()
        toolset = UrlsMcpToolset()
        val service = project.getService(IBookmarksService::class.java)
        bookmarkDatabase = service.getBookmarkDatabase()
        rootFolderId = bookmarkDatabase.getBookmarksTree().rootFolder.id

        linksFolderId = BookmarkId()
        bookmarkDatabase.modify { modifier ->
            modifier.addBookmarks(rootFolderId, listOf(
                bookmarkFolder(linksFolderId, "links").build()
            ))
        }
    }

    @Test
    fun testAddUrlBookmarkCreatesBookmarkWithUrl() {
        runBlocking {
            val result = toolset.add_url_bookmark(url = "https://example.com")

            assertThat(result.type).isEqualTo(BookmarkType.BOOKMARK)
            assertThat(result.properties[PROP_URL]).isEqualTo("https://example.com")
        }
    }

    @Test
    fun testAddUrlBookmarkUsesExplicitName() {
        runBlocking {
            val result = toolset.add_url_bookmark(url = "https://example.com", name = "Example Site")

            assertThat(result.properties["name"]).isEqualTo("Example Site")
        }
    }

    @Test
    fun testAddUrlBookmarkDefaultsNameToUrl() {
        runBlocking {
            val result = toolset.add_url_bookmark(url = "https://example.com")

            assertThat(result.properties["name"]).isEqualTo("https://example.com")
        }
    }

    @Test
    fun testAddUrlBookmarkWithComment() {
        runBlocking {
            val result = toolset.add_url_bookmark(url = "https://example.com", comment = "useful resource")

            assertThat(result.properties["comment"]).isEqualTo("useful resource")
        }
    }

    @Test
    fun testAddUrlBookmarkInSpecificFolder() {
        runBlocking {
            val result = toolset.add_url_bookmark(url = "https://example.com", parentId = linksFolderId.toString())

            assertThat(result.folderPath).isEqualTo("/links")
        }
    }

    @Test
    fun testAddUrlBookmarkWithBlankUrlFails() {
        assertMcpFails("blank") {
            runBlocking { toolset.add_url_bookmark(url = "   ") }
        }
    }

    @Test
    fun testAddUrlBookmarkWithInvalidParentIdFails() {
        assertMcpFails("not found") {
            runBlocking { toolset.add_url_bookmark(url = "https://example.com", parentId = "nonexistent") }
        }
    }

    @Test
    fun testAddUrlBookmarkWithNonFolderParentIdFails() {
        runBlocking {
            val existing = toolset.add_url_bookmark(url = "https://example.com")
            assertMcpFails("not a folder") {
                runBlocking { toolset.add_url_bookmark(url = "https://other.com", parentId = existing.id) }
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
