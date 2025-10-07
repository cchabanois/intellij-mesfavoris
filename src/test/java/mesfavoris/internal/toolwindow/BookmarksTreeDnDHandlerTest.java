package mesfavoris.internal.toolwindow;

import com.intellij.ide.dnd.DnDAction;
import com.intellij.ide.dnd.DnDDragStartBean;
import com.intellij.ide.dnd.DnDEvent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.model.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Test for BookmarksTreeDnDHandler
 */
public class BookmarksTreeDnDHandlerTest extends BasePlatformTestCase {
    private BookmarkDatabase bookmarkDatabase;
    private BookmarksTreeComponent tree;
    private BookmarksTreeDnDHandler dndHandler;
    private Disposable testDisposable;
    
    private BookmarkId rootFolderId;
    private BookmarkId folder1Id;
    private BookmarkId bookmark1Id;
    private BookmarkId bookmark2Id;
    private BookmarkId bookmark3Id;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        testDisposable = Disposer.newDisposable();
        
        // Create test data
        rootFolderId = new BookmarkId("root");
        folder1Id = new BookmarkId("folder1");
        bookmark1Id = new BookmarkId("bookmark1");
        bookmark2Id = new BookmarkId("bookmark2");
        bookmark3Id = new BookmarkId("bookmark3");
        
        BookmarksTree bookmarksTree = bookmarksTree(rootFolderId.toString())
                .addBookmarks(rootFolderId.toString(),
                    bookmark(bookmark1Id, "Bookmark 1"),
                    bookmark(bookmark2Id, "Bookmark 2"),
                    bookmarkFolder(folder1Id, "Folder 1"))
                .addBookmarks(folder1Id.toString(),
                    bookmark(bookmark3Id, "Bookmark 3"))
                .build();
        
        bookmarkDatabase = new BookmarkDatabase("test", bookmarksTree);
        tree = new BookmarksTreeComponent(bookmarkDatabase, testDisposable);
        
        dndHandler = new BookmarksTreeDnDHandler(tree, bookmarkDatabase);
        Disposer.register(testDisposable, dndHandler);
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
    public void testCanStartDraggingWithSelection() {
        // Given
        TreePath path = tree.getTreePathForBookmark(bookmark1Id).orElseThrow();
        tree.setSelectionPath(path);

        // When
        boolean canDrag = dndHandler.canStartDragging(DnDAction.MOVE, new Point(0, 0));

        // Then
        assertThat(canDrag).isTrue();
    }

    @Test
    public void testCanStartDraggingWithoutSelection() {
        // Given
        tree.clearSelection();

        // When
        boolean canDrag = dndHandler.canStartDragging(DnDAction.MOVE, new Point(0, 0));

        // Then
        assertThat(canDrag).isFalse();
    }

    @Test
    public void testStartDraggingReturnsBookmarks() {
        // Given
        TreePath path1 = tree.getTreePathForBookmark(bookmark1Id).orElseThrow();
        TreePath path2 = tree.getTreePathForBookmark(bookmark2Id).orElseThrow();
        tree.setSelectionPaths(new TreePath[]{path1, path2});

        // When
        DnDDragStartBean dragStartBean = dndHandler.startDragging(DnDAction.MOVE, new Point(0, 0));

        // Then
        assertThat(dragStartBean).isNotNull();
        assertThat(dragStartBean.getAttachedObject()).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<Bookmark> bookmarks = (List<Bookmark>) dragStartBean.getAttachedObject();
        assertThat(bookmarks).hasSize(2);
        assertThat(bookmarks.get(0).getId()).isEqualTo(bookmark1Id);
        assertThat(bookmarks.get(1).getId()).isEqualTo(bookmark2Id);
    }

    @Test
    public void testStartDraggingWithoutSelectionReturnsNull() {
        // Given
        tree.clearSelection();

        // When
        DnDDragStartBean dragStartBean = dndHandler.startDragging(DnDAction.MOVE, new Point(0, 0));

        // Then
        assertThat(dragStartBean).isNull();
    }

    @Test
    public void testDropBookmarkAfterAnother() throws Exception {
        // Given
        Bookmark bookmark1 = bookmarkDatabase.getBookmarksTree().getBookmark(bookmark1Id);
        Bookmark bookmark2 = bookmarkDatabase.getBookmarksTree().getBookmark(bookmark2Id);

        // Create a mock DnDEvent for dropping bookmark1 after bookmark2
        DnDEvent event = createMockDropEvent(List.of(bookmark1), bookmark2Id, DropPosition.AFTER);

        // When
        dndHandler.drop(event);

        // Then
        verifyBookmarkOrder(rootFolderId, bookmark2Id, bookmark1Id, folder1Id);
    }

    @Test
    public void testDropBookmarkBeforeAnother() throws Exception {
        // Given
        Bookmark bookmark2 = bookmarkDatabase.getBookmarksTree().getBookmark(bookmark2Id);
        Bookmark bookmark1 = bookmarkDatabase.getBookmarksTree().getBookmark(bookmark1Id);

        // Create a mock DnDEvent for dropping bookmark2 before bookmark1
        DnDEvent event = createMockDropEvent(List.of(bookmark2), bookmark1Id, DropPosition.BEFORE);

        // When
        dndHandler.drop(event);

        // Then
        verifyBookmarkOrder(rootFolderId, bookmark2Id, bookmark1Id, folder1Id);
    }

    @Test
    public void testDropBookmarkIntoFolder() throws Exception {
        // Given
        Bookmark bookmark1 = bookmarkDatabase.getBookmarksTree().getBookmark(bookmark1Id);
        
        // Create a mock DnDEvent for dropping bookmark1 into folder1
        DnDEvent event = createMockDropEvent(List.of(bookmark1), folder1Id, DropPosition.INTO);
        
        // When
        dndHandler.drop(event);
        
        // Then
        BookmarksTree updatedTree = bookmarkDatabase.getBookmarksTree();
        List<Bookmark> folder1Children = updatedTree.getChildren(folder1Id);
        assertThat(folder1Children).hasSize(2);
        assertThat(folder1Children.get(0).getId()).isEqualTo(bookmark3Id);
        assertThat(folder1Children.get(1).getId()).isEqualTo(bookmark1Id);
    }

    @Test
    public void testUpdateWithValidDropReturnsTrueAndSetsPossible() {
        // Given
        Bookmark bookmark1 = bookmarkDatabase.getBookmarksTree().getBookmark(bookmark1Id);
        DnDEvent event = createMockDropEvent(List.of(bookmark1), bookmark2Id, DropPosition.AFTER);

        // When
        boolean result = dndHandler.update(event);

        // Then
        assertThat(result).isTrue();
        verify(event).setDropPossible(true);
    }

    @Test
    public void testUpdateWithInvalidAttachedObjectReturnsFalse() {
        // Given
        DnDEvent event = mock(DnDEvent.class);
        when(event.getAttachedObject()).thenReturn("invalid object");
        when(event.getPoint()).thenReturn(new Point(10, 10));

        // When
        boolean result = dndHandler.update(event);

        // Then
        assertThat(result).isFalse();
        verify(event).setDropPossible(false);
    }

    // Helper methods

    private enum DropPosition {
        BEFORE, INTO, AFTER
    }

    private DnDEvent createMockDropEvent(List<Bookmark> bookmarks, BookmarkId targetId, DropPosition position) {
        DnDEvent event = mock(DnDEvent.class);
        when(event.getAttachedObject()).thenReturn(bookmarks);
        
        TreePath targetPath = tree.getTreePathForBookmark(targetId).orElseThrow();
        Rectangle bounds = tree.getPathBounds(targetPath);
        
        // Calculate point based on position
        Point point;
        Bookmark targetBookmark = bookmarkDatabase.getBookmarksTree().getBookmark(targetId);
        if (targetBookmark instanceof BookmarkFolder) {
            switch (position) {
                case BEFORE:
                    point = new Point(bounds.x + 10, bounds.y + (int)(bounds.height * 0.1));
                    break;
                case INTO:
                    point = new Point(bounds.x + 10, bounds.y + bounds.height / 2);
                    break;
                case AFTER:
                    point = new Point(bounds.x + 10, bounds.y + (int)(bounds.height * 0.9));
                    break;
                default:
                    point = new Point(bounds.x + 10, bounds.y + bounds.height / 2);
            }
        } else {
            switch (position) {
                case BEFORE:
                    point = new Point(bounds.x + 10, bounds.y + (int)(bounds.height * 0.25));
                    break;
                case AFTER:
                    point = new Point(bounds.x + 10, bounds.y + (int)(bounds.height * 0.75));
                    break;
                default:
                    point = new Point(bounds.x + 10, bounds.y + bounds.height / 2);
            }
        }
        
        when(event.getPoint()).thenReturn(point);
        
        return event;
    }

    private void verifyBookmarkOrder(BookmarkId parentId, BookmarkId... expectedOrder) {
        BookmarksTree updatedTree = bookmarkDatabase.getBookmarksTree();
        List<Bookmark> children = updatedTree.getChildren(parentId);

        assertThat(children).hasSize(expectedOrder.length);
        for (int i = 0; i < expectedOrder.length; i++) {
            assertThat(children.get(i).getId())
                    .as("Bookmark at position " + i)
                    .isEqualTo(expectedOrder[i]);
        }
    }
}

