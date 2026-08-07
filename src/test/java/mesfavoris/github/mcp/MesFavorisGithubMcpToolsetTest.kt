package mesfavoris.github.mcp
import mesfavoris.github.client.GistResponse

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.registerServiceInstance
import kotlinx.coroutines.runBlocking
import mesfavoris.github.BookmarksGithubService
import mesfavoris.github.GithubTestUser
import mesfavoris.github.client.GistApiClient
import mesfavoris.github.test.GithubConnectionRule
import mesfavoris.model.BookmarkFolder
import mesfavoris.model.BookmarkId
import mesfavoris.model.BookmarksTree
import mesfavoris.persistence.json.BookmarksTreeJsonSerializer
import mesfavoris.service.IBookmarksService
import mesfavoris.tests.commons.waits.Waiter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.StringWriter
import java.net.http.HttpClient
import java.time.Duration

class MesFavorisGithubMcpToolsetTest : BasePlatformTestCase() {

    private lateinit var toolset: MesFavorisGithubMcpToolset
    private lateinit var connectionRule: GithubConnectionRule
    private lateinit var apiClient: GistApiClient
    private val createdGistIds = mutableListOf<String>()

    @Before
    override fun setUp() {
        super.setUp()
        Assume.assumeTrue("USER1_GITHUB_TOKEN not set", GithubTestUser.USER1.token.isPresent)

        connectionRule = GithubConnectionRule(project, GithubTestUser.USER1, true)
        connectionRule.before()

        val token = GithubTestUser.USER1.token.get()
        apiClient = GistApiClient({ token }, GithubTestUser.USER1::getApiBaseUrl, HttpClient.newHttpClient())

        val mockGithubService = mock(BookmarksGithubService::class.java)
        `when`(mockGithubService.connectionManager).thenReturn(connectionRule.connectionManager)
        project.registerServiceInstance(BookmarksGithubService::class.java, mockGithubService)

        toolset = MesFavorisGithubMcpToolset()
    }

    override fun tearDown() {
        try {
            for (id in createdGistIds) {
                runCatching { apiClient.deleteGist(id) }
            }
            connectionRule.after()
        } finally {
            super.tearDown()
        }
    }

    @Test
    fun testListGithubBookmarkGistsReturnsCreatedGist() {
        val folderId = BookmarkId()
        val gist = createBookmarksGist("test-list", folderId)
        createdGistIds.add(gist.id)

        Waiter.waitUntil("Gist not visible in listing", {
            runBlocking { toolset.list_github_bookmark_gists() }.gists.any { it.gistId == gist.id }
        }, Duration.ofSeconds(30))

        runBlocking {
            val result = toolset.list_github_bookmark_gists()
            val found = result.gists.first { it.gistId == gist.id }
            assertThat(found.description).isEqualTo("mesfavoris: test-list")
            assertThat(found.url).isEqualTo(gist.html_url)
            assertThat(found.alreadyImported).isFalse()
        }
    }

    @Test
    fun testImportGithubBookmarkGist() {
        val folderId = BookmarkId()
        val gist = createBookmarksGist("test-import", folderId)
        createdGistIds.add(gist.id)

        Waiter.waitUntil("Gist not visible in listing", {
            runBlocking { toolset.list_github_bookmark_gists() }.gists.any { it.gistId == gist.id }
        }, Duration.ofSeconds(30))

        runBlocking {
            val result = toolset.import_github_bookmark_gist(gist.id)
            assertThat(result).containsIgnoringCase("imported")
        }

        val tree = project.getService(IBookmarksService::class.java).getBookmarksTree()
        assertThat(tree.getBookmark(folderId)).isNotNull()
        assertThat(tree.getBookmark(folderId)).isInstanceOf(BookmarkFolder::class.java)
    }

    @Test
    fun testImportGithubBookmarkFromUrl() {
        val folderId = BookmarkId()
        val gist = createBookmarksGist("test-import-url", folderId)
        createdGistIds.add(gist.id)
        val url = "https://gist.github.com/${gist.owner.login}/${gist.id}"

        Waiter.waitUntil("Gist not visible in listing", {
            runBlocking { toolset.list_github_bookmark_gists() }.gists.any { it.gistId == gist.id }
        }, Duration.ofSeconds(30))

        runBlocking {
            val result = toolset.import_github_bookmark_from_url(url)
            assertThat(result).containsIgnoringCase("imported")
        }

        val tree = project.getService(IBookmarksService::class.java).getBookmarksTree()
        assertThat(tree.getBookmark(folderId)).isNotNull()
    }

    @Test
    fun testListGithubBookmarkGistsMarksAlreadyImportedGists() {
        val folderId = BookmarkId()
        val gist = createBookmarksGist("test-already-imported", folderId)
        createdGistIds.add(gist.id)

        Waiter.waitUntil("Gist not visible in listing", {
            runBlocking { toolset.list_github_bookmark_gists() }.gists.any { it.gistId == gist.id }
        }, Duration.ofSeconds(30))

        runBlocking { toolset.import_github_bookmark_gist(gist.id) }

        runBlocking {
            val result = toolset.list_github_bookmark_gists()
            val found = result.gists.first { it.gistId == gist.id }
            assertThat(found.alreadyImported).isTrue()
        }
    }

    @Test
    fun testImportFromBlankUrlFails() {
        assertMcpFails("blank") {
            runBlocking { toolset.import_github_bookmark_from_url("   ") }
        }
    }

    @Test
    fun testImportFromInvalidUrlFails() {
        assertMcpFails("Gist ID") {
            runBlocking { toolset.import_github_bookmark_from_url("https://example.com/notgist") }
        }
    }

    private fun createBookmarksGist(name: String, folderId: BookmarkId = BookmarkId()): GistResponse {
        val tree = BookmarksTree(BookmarkFolder(folderId, mapOf("name" to name)))
        val writer = StringWriter()
        BookmarksTreeJsonSerializer(false).serialize(tree, folderId, writer)
        return apiClient.createGist("mesfavoris: $name", "bookmarks.json", writer.toString())
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
