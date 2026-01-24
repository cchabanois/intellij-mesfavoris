package mesfavoris.internal.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.BookmarksException;
import mesfavoris.model.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class BookmarksTreeModelTest extends BasePlatformTestCase {
    private BookmarkDatabase bookmarkDatabase;
    private BookmarksTreeModel treeModel;
    private Disposable testDisposable;
    private TreeModelListener treeModelListener;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        testDisposable = Disposer.newDisposable();
        bookmarkDatabase = new BookmarkDatabase("test", createInitialBookmarksTree());
        treeModel = new BookmarksTreeModel(bookmarkDatabase, testDisposable);
        treeModelListener = mock(TreeModelListener.class);
        treeModel.addTreeModelListener(treeModelListener);
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
        BookmarkTreeNode rootNode = treeModel.getRoot();

        // Then
        assertThat(rootNode).isNotNull();
        assertThat(rootNode.getBookmark()).isInstanceOf(BookmarkFolder.class);
        assertThat(rootNode.getId().toString()).isEqualTo("root");
        assertThat(rootNode.getBookmark().getPropertyValue(Bookmark.PROPERTY_NAME)).isEqualTo("root");
    }

    @Test
    public void testGetChildrenWithBookmarkFolder() {
        // Given
        BookmarkTreeNode rootNode = treeModel.getRoot();

        // When
        List<BookmarkTreeNode> children = treeModel.getChildren(rootNode);

        // Then
        assertThat(children).hasSize(2);
        assertThat(children.get(0).getId().toString()).isEqualTo("folder1");
        assertThat(children.get(1).getId().toString()).isEqualTo("folder2");
    }

    @Test
    public void testGetChildrenWithNonFolder() {
        // Given
        BookmarkTreeNode bookmarkNode = new BookmarkTreeNode(bookmark("bookmark1").build());

        // When
        List<BookmarkTreeNode> children = treeModel.getChildren(bookmarkNode);

        // Then
        assertThat(children).isEmpty();
    }

    @Test
    public void testIsLeafWithBookmarkFolder() {
        // Given
        BookmarkTreeNode folderNode = new BookmarkTreeNode(bookmarkFolder("folder").build());

        // When
        boolean isLeaf = treeModel.isLeaf(folderNode);

        // Then
        assertThat(isLeaf).isFalse();
    }

    @Test
    public void testIsLeafWithBookmark() {
        // Given
        BookmarkTreeNode bookmarkNode = new BookmarkTreeNode(bookmark("bookmark").build());

        // When
        boolean isLeaf = treeModel.isLeaf(bookmarkNode);

        // Then
        assertThat(isLeaf).isTrue();
    }

    @Test
    public void testGetTreePathForBookmarkWithBookmarkId() {
        // Given
        BookmarkId bookmarkId = new BookmarkId("folder1");

        // When
        Optional<TreePath> optionalTreePath = treeModel.getTreePathForBookmark(bookmarkId);

        // Then
        assertThat(optionalTreePath).isPresent();
        TreePath treePath = optionalTreePath.get();
        assertThat(treePath.getPathCount()).isEqualTo(2);
        assertThat(((BookmarkTreeNode) treePath.getPathComponent(0)).getId().toString()).isEqualTo("root");
        assertThat(((BookmarkTreeNode) treePath.getPathComponent(1)).getId().toString()).isEqualTo("folder1");
    }

    @Test
    public void testGetTreePathForBookmarkWithUnknownBookmarkId() {
        // Given
        BookmarkId unknownBookmarkId = new BookmarkId("unknownBookmark");

        // When
        Optional<TreePath> optionalTreePath = treeModel.getTreePathForBookmark(unknownBookmarkId);

        // Then
        assertThat(optionalTreePath).isEmpty();
    }

    @Test
    public void testValueForPathChangedRenameBookmark() throws BookmarksException {
        // Given
        BookmarkId bookmarkId = new BookmarkId("folder1");
        Optional<TreePath> optionalTreePath = treeModel.getTreePathForBookmark(bookmarkId);
        assertThat(optionalTreePath).isPresent();
        TreePath treePath = optionalTreePath.get();
        String newName = "Renamed Folder";

        // When
        treeModel.valueForPathChanged(treePath, newName);

        // Then
        verifyBookmarkRenamed(bookmarkId, newName);
    }

    @Test
    public void testBookmarksListenerOnBookmarksAdded() throws Exception {
        // Given
        BookmarkId parentId = new BookmarkId("folder1");
        Bookmark newBookmark = bookmark("newBookmark").build();
        TreePath parentPath = treeModel.getTreePathForBookmark(parentId).get();
        BookmarkTreeNode parentNode = treeModel.getBookmarkTreeNode(parentPath);
        int initialChildrenCount = treeModel.getChildren(parentNode).size();

        // When
        addBookmarkToDatabase(parentId, newBookmark);

        // Then
        List<BookmarkTreeNode> children = treeModel.getChildren(parentNode);
        assertThat(children).hasSize(initialChildrenCount + 1);
        assertThat(children.stream().anyMatch(node -> node.getId().equals(newBookmark.getId()))).isTrue();
    }

    @Test
    public void testBookmarksListenerOnBookmarkDeleted() throws Exception {
        // Given
        BookmarkId bookmarkToDeleteId = new BookmarkId("folder2");
        BookmarkTreeNode rootNode = treeModel.getRoot();
        int initialChildrenCount = treeModel.getChildren(rootNode).size();

        // When
        deleteBookmarkFromDatabase(bookmarkToDeleteId);

        // Then
        List<BookmarkTreeNode> children = treeModel.getChildren(rootNode);
        assertThat(children).hasSize(initialChildrenCount - 1);
        assertThat(children.stream().noneMatch(node -> node.getId().equals(bookmarkToDeleteId))).isTrue();
    }

    @Test
    public void testBookmarksListenerOnBookmarkPropertiesModified() throws Exception {
        // Given
        BookmarkId bookmarkId = new BookmarkId("folder1");
        String newName = "Modified Folder";

        // When
        modifyBookmarkProperties(bookmarkId, newName);

        // Then
        Bookmark modifiedBookmark = bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId);
        assertThat(modifiedBookmark.getPropertyValue(Bookmark.PROPERTY_NAME)).isEqualTo(newName);
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

    private void verifyBookmarkRenamed(BookmarkId bookmarkId, String expectedName) {
        Bookmark renamedBookmark = bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId);
        assertThat(renamedBookmark.getPropertyValue(Bookmark.PROPERTY_NAME)).isEqualTo(expectedName);
    }

}
