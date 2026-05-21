package mesfavoris.notes.internal.mcp

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import mesfavoris.internal.mcp.BookmarkType
import mesfavoris.model.BookmarkDatabase
import mesfavoris.model.BookmarkId
import mesfavoris.notes.NoteBookmarkProperties.PROP_NOTES
import mesfavoris.service.IBookmarksService
import mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class NotesMcpToolsetTest : BasePlatformTestCase() {

    private lateinit var toolset: NotesMcpToolset
    private lateinit var bookmarkDatabase: BookmarkDatabase
    private lateinit var rootFolderId: BookmarkId
    private lateinit var notesFolderId: BookmarkId

    @Before
    override fun setUp() {
        super.setUp()
        toolset = NotesMcpToolset()
        val service = project.getService(IBookmarksService::class.java)
        bookmarkDatabase = service.getBookmarkDatabase()
        rootFolderId = bookmarkDatabase.getBookmarksTree().rootFolder.id

        notesFolderId = BookmarkId()
        bookmarkDatabase.modify { modifier ->
            modifier.addBookmarks(rootFolderId, listOf(
                bookmarkFolder(notesFolderId, "notes").build()
            ))
        }
    }

    @Test
    fun testAddNoteCreatesBookmarkWithContent() {
        runBlocking {
            val result = toolset.add_note(name = "My Note", content = "# Hello\nSome text")

            assertThat(result.type).isEqualTo(BookmarkType.BOOKMARK)
            assertThat(result.properties[PROP_NOTES]).isEqualTo("# Hello\nSome text")
        }
    }

    @Test
    fun testAddNoteUsesExplicitName() {
        runBlocking {
            val result = toolset.add_note(name = "Meeting notes", content = "# Team meeting")

            assertThat(result.properties["name"]).isEqualTo("Meeting notes")
        }
    }

    @Test
    fun testAddNoteDerivesNameFromMarkdownHeading() {
        runBlocking {
            val result = toolset.add_note(content = "# My Heading\nSome content")

            assertThat(result.properties["name"]).isEqualTo("My Heading")
        }
    }

    @Test
    fun testAddNoteDerivesNameFromFirstLine() {
        runBlocking {
            val result = toolset.add_note(content = "First line\nSecond line")

            assertThat(result.properties["name"]).isEqualTo("First line")
        }
    }

    @Test
    fun testAddNoteDefaultsToNewNoteWhenNoContent() {
        runBlocking {
            val result = toolset.add_note()

            assertThat(result.properties["name"]).isEqualTo("New Note")
            assertThat(result.properties[PROP_NOTES]).isEqualTo("")
        }
    }

    @Test
    fun testAddNoteWithComment() {
        runBlocking {
            val result = toolset.add_note(name = "Todo", content = "- item 1", comment = "work items")

            assertThat(result.properties["comment"]).isEqualTo("work items")
        }
    }

    @Test
    fun testAddNoteInSpecificFolder() {
        runBlocking {
            val result = toolset.add_note(name = "Note", parentId = notesFolderId.toString())

            assertThat(result.folderPath).isEqualTo("/notes")
        }
    }

    @Test
    fun testAddNoteWithInvalidParentIdFails() {
        assertMcpFails("not found") {
            runBlocking { toolset.add_note(name = "Note", parentId = "nonexistent") }
        }
    }

    @Test
    fun testAddNoteWithNonFolderParentIdFails() {
        runBlocking {
            val existing = toolset.add_note(name = "Existing")
            assertMcpFails("not a folder") {
                runBlocking { toolset.add_note(name = "New", parentId = existing.id) }
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
