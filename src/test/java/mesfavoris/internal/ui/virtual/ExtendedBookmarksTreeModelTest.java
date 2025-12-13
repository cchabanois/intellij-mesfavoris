package mesfavoris.internal.ui.virtual;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.BookmarksException;
import mesfavoris.internal.toolwindow.BookmarksTreeModel;
import mesfavoris.model.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ExtendedBookmarksTreeModelTest extends BasePlatformTestCase {
    private BookmarkDatabase bookmarkDatabase;
    private BookmarksTreeModel bookmarksTreeModel;
    private ExtendedBookmarksTreeModel extendedTreeModel;
    private Disposable testDisposable;
    private TestVirtualBookmarkFolder virtualFolder;
    private TreeModelListener treeModelListener;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        testDisposable = Disposer.newDisposable();
        bookmarkDatabase = new BookmarkDatabase("test", createInitialBookmarksTree());
        bookmarksTreeModel = new BookmarksTreeModel(bookmarkDatabase, testDisposable);

        // Create a virtual folder under folder1
        BookmarkId folder1Id = new BookmarkId("folder1");
        virtualFolder = new TestVirtualBookmarkFolder(folder1Id, "Virtual Folder");

        List<VirtualBookmarkFolder> virtualFolders = Collections.singletonList(virtualFolder);
        extendedTreeModel = new ExtendedBookmarksTreeModel(bookmarksTreeModel, virtualFolders, testDisposable);

        treeModelListener = mock(TreeModelListener.class);
        extendedTreeModel.addTreeModelListener(treeModelListener);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        try {
            if (testDisposable != null) {
                Disposer.dispose(testDisposable);
            }
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void testGetRoot() {
        // When
        Object root = extendedTreeModel.getRoot();

        // Then
        assertThat(root).isNotNull();
        assertThat(root).isInstanceOf(BookmarkFolder.class);
        BookmarkFolder rootFolder = (BookmarkFolder) root;
        assertThat(rootFolder.getId().toString()).isEqualTo("root");
    }

    @Test
    public void testGetChildrenWithRegularBookmarkFolder() {
        // Given
        BookmarkFolder rootFolder = (BookmarkFolder) extendedTreeModel.getRoot();

        // When
        List<Object> children = extendedTreeModel.getChildren(rootFolder);

        // Then
        assertThat(children).hasSize(2);
        assertThat(children.get(0)).isInstanceOf(Bookmark.class);
        assertThat(children.get(1)).isInstanceOf(Bookmark.class);
    }

    @Test
    public void testGetChildrenIncludesVirtualFolder() {
        // Given
        BookmarkFolder folder1 = (BookmarkFolder) bookmarkDatabase.getBookmarksTree()
                .getBookmark(new BookmarkId("folder1"));

        // When
        List<Object> children = extendedTreeModel.getChildren(folder1);

        // Then
        assertThat(children).hasSize(2); // 1 regular bookmark + 1 virtual folder
        assertThat(children.stream().filter(c -> c instanceof Bookmark).count()).isEqualTo(1);
        assertThat(children.stream().filter(c -> c instanceof VirtualBookmarkFolder).count()).isEqualTo(1);
    }

    @Test
    public void testGetChildrenOfVirtualFolder() {
        // Given
        Bookmark bookmark1 = bookmarkDatabase.getBookmarksTree().getBookmark(new BookmarkId("bookmark1"));
        BookmarkLink link = new BookmarkLink(virtualFolder.getBookmarkFolder().getId(), bookmark1);
        virtualFolder.addChild(link);

        // When
        List<Object> children = extendedTreeModel.getChildren(virtualFolder);

        // Then
        assertThat(children).hasSize(1);
        assertThat(children.get(0)).isInstanceOf(BookmarkLink.class);
        BookmarkLink bookmarkLink = (BookmarkLink) children.get(0);
        assertThat(bookmarkLink.getBookmark().getId()).isEqualTo(bookmark1.getId());
    }

    @Test
    public void testIsLeafWithBookmark() {
        // Given
        Bookmark bookmark = bookmarkDatabase.getBookmarksTree().getBookmark(new BookmarkId("bookmark1"));

        // When
        boolean isLeaf = extendedTreeModel.isLeaf(bookmark);

        // Then
        assertThat(isLeaf).isTrue();
    }

    @Test
    public void testIsLeafWithBookmarkFolder() {
        // Given
        BookmarkFolder folder = (BookmarkFolder) bookmarkDatabase.getBookmarksTree()
                .getBookmark(new BookmarkId("folder1"));

        // When
        boolean isLeaf = extendedTreeModel.isLeaf(folder);

        // Then
        assertThat(isLeaf).isFalse();
    }

    @Test
    public void testIsLeafWithVirtualFolder() {
        // When
        boolean isLeaf = extendedTreeModel.isLeaf(virtualFolder);

        // Then
        assertThat(isLeaf).isFalse();
    }

    @Test
    public void testIsLeafWithBookmarkLink() {
        // Given
        Bookmark bookmark1 = bookmarkDatabase.getBookmarksTree().getBookmark(new BookmarkId("bookmark1"));
        BookmarkLink link = new BookmarkLink(virtualFolder.getBookmarkFolder().getId(), bookmark1);

        // When
        boolean isLeaf = extendedTreeModel.isLeaf(link);

        // Then
        assertThat(isLeaf).isTrue();
    }

    @Test
    public void testTreeModelListenerNotifiedOnBookmarkAdded() throws Exception {
        // Given
        BookmarkId parentId = new BookmarkId("folder1");
        Bookmark newBookmark = bookmark("newBookmark").build();

        // When
        addBookmarkToDatabase(parentId, newBookmark);

        // Wait for EDT to process events
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        // Then
        verify(treeModelListener, atLeastOnce()).treeStructureChanged(any(TreeModelEvent.class));
    }

    @Test
    public void testTreeModelListenerNotifiedOnBookmarkDeleted() throws Exception {
        // Given
        BookmarkId bookmarkToDeleteId = new BookmarkId("bookmark1");

        // When
        deleteBookmarkFromDatabase(bookmarkToDeleteId);

        // Wait for EDT to process events
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        // Then
        verify(treeModelListener, atLeastOnce()).treeStructureChanged(any(TreeModelEvent.class));
    }

    @Test
    public void testTreeModelListenerNotifiedOnBookmarkPropertiesModified() throws Exception {
        // Given
        BookmarkId bookmarkId = new BookmarkId("folder1");
        String newName = "Modified Folder";

        // When
        modifyBookmarkProperties(bookmarkId, newName);

        // Wait for EDT to process events
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        // Then
        verify(treeModelListener, atLeastOnce()).treeStructureChanged(any(TreeModelEvent.class));
    }

    @Test
    public void testTreeModelListenerNotifiedOnVirtualFolderChildrenChanged() throws Exception {
        // Given
        Bookmark bookmark1 = bookmarkDatabase.getBookmarksTree().getBookmark(new BookmarkId("bookmark1"));
        BookmarkLink link = new BookmarkLink(virtualFolder.getBookmarkFolder().getId(), bookmark1);

        // When
        virtualFolder.addChild(link);

        // Wait for EDT to process events
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        // Then
        verify(treeModelListener, atLeastOnce()).treeStructureChanged(any(TreeModelEvent.class));
    }

    @Test
    public void testVirtualFolderChangeNotifiesWithCorrectTreePath() throws Exception {
        // Given
        Bookmark bookmark1 = bookmarkDatabase.getBookmarksTree().getBookmark(new BookmarkId("bookmark1"));
        BookmarkLink link = new BookmarkLink(virtualFolder.getBookmarkFolder().getId(), bookmark1);

        // When
        virtualFolder.addChild(link);

        // Wait for EDT to process events
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        // Then: Verify that treeStructureChanged was called with a non-null TreePath
        // (not with null which would collapse the entire tree)
        verify(treeModelListener, atLeastOnce()).treeStructureChanged(argThat(event -> {
            // The TreePath should not be null (which would refresh the entire tree)
            // It should be the path to the virtual folder
            return event.getTreePath() != null &&
                   event.getTreePath().getLastPathComponent() == virtualFolder;
        }));
    }

    private BookmarksTree createInitialBookmarksTree() {
        return bookmarksTree("root")
                .addBookmarks("root", bookmarkFolder("folder1"), bookmarkFolder("folder2"))
                .addBookmarks("folder1", bookmark("bookmark1"))
                .build();
    }

    private void addBookmarkToDatabase(BookmarkId parentId, Bookmark bookmark) throws BookmarksException {
        bookmarkDatabase.modify(modifier -> modifier.addBookmarks(parentId, Collections.singletonList(bookmark)));
    }

    private void deleteBookmarkFromDatabase(BookmarkId bookmarkId) throws BookmarksException {
        bookmarkDatabase.modify(modifier -> modifier.deleteBookmark(bookmarkId, false));
    }

    private void modifyBookmarkProperties(BookmarkId bookmarkId, String newName) throws BookmarksException {
        bookmarkDatabase.modify(modifier -> modifier.setPropertyValue(bookmarkId, Bookmark.PROPERTY_NAME, newName));
    }

    /**
     * Test implementation of VirtualBookmarkFolder for testing purposes
     */
    private static class TestVirtualBookmarkFolder extends VirtualBookmarkFolder {
        private final List<BookmarkLink> children = new ArrayList<>();

        public TestVirtualBookmarkFolder(BookmarkId parentId, String name) {
            super(parentId, name);
        }

        @Override
        public List<BookmarkLink> getChildren() {
            return new ArrayList<>(children);
        }

        public void addChild(BookmarkLink child) {
            children.add(child);
            fireChildrenChanged();
        }

        public void removeChild(BookmarkLink child) {
            children.remove(child);
            fireChildrenChanged();
        }

        @Override
        protected void initListening() {
            // No-op for test
        }

        @Override
        protected void stopListening() {
            // No-op for test
        }
    }
}

