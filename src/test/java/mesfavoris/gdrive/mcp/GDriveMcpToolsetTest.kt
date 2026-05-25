package mesfavoris.gdrive.mcp

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.registerServiceInstance
import kotlinx.coroutines.runBlocking
import mesfavoris.gdrive.BookmarksGDriveService
import mesfavoris.gdrive.GDriveTestUser
import mesfavoris.gdrive.operations.BookmarkFileConstants.MESFAVORIS_MIME_TYPE
import mesfavoris.gdrive.operations.CreateFileOperation
import mesfavoris.gdrive.test.GDriveConnectionRule
import mesfavoris.model.BookmarkDatabase
import mesfavoris.model.BookmarkFolder
import mesfavoris.model.BookmarkId
import mesfavoris.model.BookmarksTree
import mesfavoris.persistence.json.BookmarksTreeJsonSerializer
import mesfavoris.service.IBookmarksService
import mesfavoris.tests.commons.waits.Waiter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.time.Duration

class MesFavorisGDriveMcpToolsetTest : BasePlatformTestCase() {

    private lateinit var toolset: MesFavorisGDriveMcpToolset
    private lateinit var gdriveConnectionRule: GDriveConnectionRule
    private lateinit var bookmarkDatabase: BookmarkDatabase
    private lateinit var rootFolderId: BookmarkId

    @Before
    override fun setUp() {
        super.setUp()
        gdriveConnectionRule = GDriveConnectionRule(project, GDriveTestUser.USER1, true)
        gdriveConnectionRule.before()

        val mockGDriveService = mock(BookmarksGDriveService::class.java)
        `when`(mockGDriveService.connectionManager).thenReturn(gdriveConnectionRule.gDriveConnectionManager)
        project.registerServiceInstance(BookmarksGDriveService::class.java, mockGDriveService)

        toolset = MesFavorisGDriveMcpToolset()
        val service = project.getService(IBookmarksService::class.java)
        bookmarkDatabase = service.getBookmarkDatabase()
        rootFolderId = bookmarkDatabase.getBookmarksTree().rootFolder.id
    }

    override fun tearDown() {
        try {
            gdriveConnectionRule.after()
        } finally {
            super.tearDown()
        }
    }

    @Test
    fun testListGDriveBookmarkFilesReturnsCreatedFile() {
        val file = createBookmarksFile("test-list.json")

        Waiter.waitUntil("File not visible in GDrive listing", {
            runBlocking { toolset.list_gdrive_bookmark_files() }.files.any { it.fileId == file.id }
        }, Duration.ofSeconds(20))

        runBlocking {
            val result = toolset.list_gdrive_bookmark_files()
            val found = result.files.first { it.fileId == file.id }
            assertThat(found.title).isEqualTo("test-list.json")
            assertThat(found.alreadyImported).isFalse()
        }
    }

    @Test
    fun testImportGDriveBookmarkFile() {
        val folderId = BookmarkId()
        val file = createBookmarksFile("test-import.json", folderId)

        Waiter.waitUntil("File not visible in GDrive listing", {
            runBlocking { toolset.list_gdrive_bookmark_files() }.files.any { it.fileId == file.id }
        }, Duration.ofSeconds(20))

        runBlocking {
            val result = toolset.import_gdrive_bookmark_file(file.id)
            assertThat(result).containsIgnoringCase("imported")
        }

        val importedBookmark = bookmarkDatabase.getBookmarksTree().getBookmark(folderId)
        assertThat(importedBookmark).isNotNull()
        assertThat(importedBookmark).isInstanceOf(BookmarkFolder::class.java)
    }

    @Test
    fun testImportGDriveBookmarkFromUrl() {
        val folderId = BookmarkId()
        val file = createBookmarksFile("test-import-url.json", folderId)
        val url = "https://drive.google.com/file/d/${file.id}/view?usp=sharing"

        Waiter.waitUntil("File not visible in GDrive listing", {
            runBlocking { toolset.list_gdrive_bookmark_files() }.files.any { it.fileId == file.id }
        }, Duration.ofSeconds(20))

        runBlocking {
            val result = toolset.import_gdrive_bookmark_from_url(url)
            assertThat(result).containsIgnoringCase("imported")
        }

        assertThat(bookmarkDatabase.getBookmarksTree().getBookmark(folderId)).isNotNull()
    }

    @Test
    fun testListGDriveBookmarkFilesMarksAlreadyImportedFiles() {
        val folderId = BookmarkId()
        val file = createBookmarksFile("test-already-imported.json", folderId)

        Waiter.waitUntil("File not visible in GDrive listing", {
            runBlocking { toolset.list_gdrive_bookmark_files() }.files.any { it.fileId == file.id }
        }, Duration.ofSeconds(20))

        runBlocking { toolset.import_gdrive_bookmark_file(file.id) }

        runBlocking {
            val result = toolset.list_gdrive_bookmark_files()
            val found = result.files.first { it.fileId == file.id }
            assertThat(found.alreadyImported).isTrue()
        }
    }

    @Test
    fun testImportFromBlankUrlFails() {
        assertMcpFails("blank") {
            runBlocking { toolset.import_gdrive_bookmark_from_url("   ") }
        }
    }

    @Test
    fun testImportFromInvalidUrlFails() {
        assertMcpFails("file ID") {
            runBlocking { toolset.import_gdrive_bookmark_from_url("https://example.com/notgdrive") }
        }
    }

    private fun createBookmarksFile(
        name: String,
        folderId: BookmarkId = BookmarkId()
    ): com.google.api.services.drive.model.File {
        val tree = BookmarksTree(BookmarkFolder(folderId, mapOf("name" to name)))
        val writer = StringWriter()
        BookmarksTreeJsonSerializer(false).serialize(tree, folderId, writer)
        val content = writer.toString().toByteArray(StandardCharsets.UTF_8)

        val createOp = CreateFileOperation(gdriveConnectionRule.drive)
        return createOp.createFile(
            gdriveConnectionRule.applicationFolderId,
            name,
            MESFAVORIS_MIME_TYPE,
            content,
            EmptyProgressIndicator()
        )
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
