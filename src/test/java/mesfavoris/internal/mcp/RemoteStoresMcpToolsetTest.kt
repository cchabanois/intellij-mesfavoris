package mesfavoris.internal.mcp

import com.intellij.icons.AllIcons
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import mesfavoris.internal.remote.InMemoryRemoteBookmarksStore
import mesfavoris.model.BookmarkDatabase
import mesfavoris.model.BookmarkId
import mesfavoris.remote.AbstractRemoteBookmarksStore
import mesfavoris.remote.IRemoteBookmarksStore
import mesfavoris.remote.RemoteBookmarksStoreExtension
import mesfavoris.remote.internal.mcp.RemoteStoresMcpToolset
import mesfavoris.service.IBookmarksService
import mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark
import mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import javax.swing.Icon

class RemoteStoresMcpToolsetTest : BasePlatformTestCase() {

    private lateinit var toolset: RemoteStoresMcpToolset
    private lateinit var bookmarkDatabase: BookmarkDatabase
    private lateinit var rootFolderId: BookmarkId
    private lateinit var syncFolderId: BookmarkId
    private lateinit var remoteStore: InMemoryRemoteBookmarksStore
    private lateinit var storeId: String

    @Before
    override fun setUp() {
        super.setUp()
        toolset = RemoteStoresMcpToolset()
        val service = project.getService(IBookmarksService::class.java)
        bookmarkDatabase = service.getBookmarkDatabase()
        rootFolderId = bookmarkDatabase.getBookmarksTree().rootFolder.id

        remoteStore = InMemoryRemoteBookmarksStore(project)
        storeId = remoteStore.descriptor.id()

        val extension = object : RemoteBookmarksStoreExtension {
            override fun getId() = storeId
            override fun getLabel() = "In Memory"
            override fun getIcon(): Icon = AllIcons.General.Information
            override fun getOverlayIcon(): Icon = AllIcons.General.Information
            override fun createStore(project: Project): AbstractRemoteBookmarksStore = remoteStore
        }
        ExtensionTestUtil.maskExtensions(RemoteBookmarksStoreExtension.EP_NAME, listOf(extension), testRootDisposable)

        syncFolderId = BookmarkId()
        bookmarkDatabase.modify { modifier ->
            modifier.addBookmarks(rootFolderId, listOf(
                bookmarkFolder(syncFolderId, "myFolder").build()
            ))
        }
    }

    @Test
    fun testListRemoteStoresReturnsStore() {
        runBlocking {
            val result = toolset.list_remote_stores()

            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].id).isEqualTo(storeId)
            assertThat(result.stores[0].state).isEqualTo(RemoteStoresMcpToolset.RemoteStoreState.DISCONNECTED)
        }
    }

    @Test
    fun testConnectRemoteStore() {
        runBlocking {
            val result = toolset.connect_remote_store(storeId)

            assertThat(result).containsIgnoringCase("connected")
            assertThat(remoteStore.state).isEqualTo(IRemoteBookmarksStore.State.connected)
        }
    }

    @Test
    fun testConnectAlreadyConnectedStore() {
        remoteStore.connect(EmptyProgressIndicator())
        runBlocking {
            val result = toolset.connect_remote_store(storeId)

            assertThat(result).containsIgnoringCase("already connected")
        }
    }

    @Test
    fun testDisconnectRemoteStore() {
        remoteStore.connect(EmptyProgressIndicator())
        runBlocking {
            val result = toolset.disconnect_remote_store(storeId)

            assertThat(result).containsIgnoringCase("disconnected")
            assertThat(remoteStore.state).isEqualTo(IRemoteBookmarksStore.State.disconnected)
        }
    }

    @Test
    fun testDisconnectAlreadyDisconnectedStore() {
        runBlocking {
            val result = toolset.disconnect_remote_store(storeId)

            assertThat(result).containsIgnoringCase("already disconnected")
        }
    }

    @Test
    fun testListRemoteStoresAfterConnect() {
        remoteStore.connect(EmptyProgressIndicator())
        runBlocking {
            val result = toolset.list_remote_stores()

            assertThat(result.stores[0].state).isEqualTo(RemoteStoresMcpToolset.RemoteStoreState.CONNECTED)
        }
    }

    @Test
    fun testConnectUnknownStoreFails() {
        assertMcpFails("not found") {
            runBlocking { toolset.connect_remote_store("unknownStore") }
        }
    }

    @Test
    fun testListRemoteFoldersEmpty() {
        runBlocking {
            val result = toolset.list_remote_folders()

            assertThat(result.folders).isEmpty()
        }
    }

    @Test
    fun testAddToRemoteStoreAndListFolders() {
        remoteStore.connect(EmptyProgressIndicator())
        runBlocking {
            toolset.add_to_remote_store(storeId, syncFolderId.toString())
            val result = toolset.list_remote_folders()

            assertThat(result.folders).hasSize(1)
            assertThat(result.folders[0].storeId).isEqualTo(storeId)
            assertThat(result.folders[0].bookmarkFolderId).isEqualTo(syncFolderId.toString())
        }
    }

    @Test
    fun testListRemoteFoldersByStoreId() {
        remoteStore.connect(EmptyProgressIndicator())
        runBlocking {
            toolset.add_to_remote_store(storeId, syncFolderId.toString())
            val result = toolset.list_remote_folders(storeId)

            assertThat(result.folders).hasSize(1)
        }
    }

    @Test
    fun testRemoveFromRemoteStore() {
        remoteStore.connect(EmptyProgressIndicator())
        runBlocking {
            toolset.add_to_remote_store(storeId, syncFolderId.toString())
            toolset.remove_from_remote_store(storeId, syncFolderId.toString())
            val result = toolset.list_remote_folders()

            assertThat(result.folders).isEmpty()
        }
    }

    @Test
    fun testAddToRemoteStoreWithNonExistentFolderFails() {
        remoteStore.connect(EmptyProgressIndicator())
        assertMcpFails("not found") {
            runBlocking { toolset.add_to_remote_store(storeId, "nonexistent") }
        }
    }

    @Test
    fun testAddNonFolderBookmarkToRemoteStoreFails() {
        remoteStore.connect(EmptyProgressIndicator())
        val bookmarkId = BookmarkId()
        bookmarkDatabase.modify { modifier ->
            modifier.addBookmarks(rootFolderId, listOf(
                bookmark(bookmarkId, "myBookmark").build()
            ))
        }
        assertMcpFails("not a folder") {
            runBlocking { toolset.add_to_remote_store(storeId, bookmarkId.toString()) }
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
