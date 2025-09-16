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
        BookmarkFolder root = treeModel.getRoot();

        // Then
        assertThat(root).isNotNull();
        assertThat(root.getId().toString()).isEqualTo("root");
        assertThat(root.getPropertyValue(Bookmark.PROPERTY_NAME)).isEqualTo("root");
    }

    @Test
    public void testGetChildrenWithBookmarkFolder() {
        // Given
        BookmarkFolder rootFolder = treeModel.getRoot();

        // When
        List<Bookmark> children = treeModel.getChildren(rootFolder);

        // Then
        assertThat(children).hasSize(2);
        assertThat(children.get(0).getId().toString()).isEqualTo("folder1");
        assertThat(children.get(1).getId().toString()).isEqualTo("folder2");
    }

    @Test
    public void testGetChildrenWithNonFolder() {
        // Given
        Bookmark bookmark = bookmark("bookmark1").build();

        // When
        List<Bookmark> children = treeModel.getChildren(bookmark);

        // Then
        assertThat(children).isEmpty();
    }

    @Test
    public void testIsLeafWithBookmarkFolder() {
        // Given
        BookmarkFolder folder = bookmarkFolder("folder").build();

        // When
        boolean isLeaf = treeModel.isLeaf(folder);

        // Then
        assertThat(isLeaf).isFalse();
    }

    @Test
    public void testIsLeafWithBookmark() {
        // Given
        Bookmark bookmark = bookmark("bookmark").build();

        // When
        boolean isLeaf = treeModel.isLeaf(bookmark);

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
        assertThat(((Bookmark) treePath.getPathComponent(0)).getId().toString()).isEqualTo("root");
        assertThat(((Bookmark) treePath.getPathComponent(1)).getId().toString()).isEqualTo("folder1");
    }

    @Test
    public void testGetTreePathForBookmarkWithBookmark() {
        // Given
        Bookmark bookmark = bookmarkDatabase.getBookmarksTree().getBookmark(new BookmarkId("folder1"));

        // When
        Optional<TreePath> optionalTreePath = treeModel.getTreePathForBookmark(bookmark);

        // Then
        assertThat(optionalTreePath).isPresent();
        TreePath treePath = optionalTreePath.get();
        assertThat(treePath.getPathCount()).isEqualTo(2);
        assertThat(((Bookmark) treePath.getPathComponent(0)).getId().toString()).isEqualTo("root");
        assertThat(((Bookmark) treePath.getPathComponent(1)).getId().toString()).isEqualTo("folder1");
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
        int initialChildrenCount = treeModel.getChildren(bookmarkDatabase.getBookmarksTree().getBookmark(parentId)).size();

        // When
        addBookmarkToDatabase(parentId, newBookmark);

        // Then
        List<Bookmark> children = treeModel.getChildren(bookmarkDatabase.getBookmarksTree().getBookmark(parentId));
        assertThat(children).hasSize(initialChildrenCount + 1);
        assertThat(children.stream().anyMatch(b -> b.getId().equals(newBookmark.getId()))).isTrue();
    }

    @Test
    public void testBookmarksListenerOnBookmarkDeleted() throws Exception {
        // Given
        BookmarkId bookmarkToDeleteId = new BookmarkId("folder2");
        BookmarkFolder rootFolder = treeModel.getRoot();
        int initialChildrenCount = treeModel.getChildren(rootFolder).size();

        // When
        deleteBookmarkFromDatabase(bookmarkToDeleteId);

        // Then
        List<Bookmark> children = treeModel.getChildren(rootFolder);
        assertThat(children).hasSize(initialChildrenCount - 1);
        assertThat(children.stream().noneMatch(b -> b.getId().equals(bookmarkToDeleteId))).isTrue();
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
