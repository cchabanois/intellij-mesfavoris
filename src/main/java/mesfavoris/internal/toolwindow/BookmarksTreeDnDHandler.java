package mesfavoris.internal.toolwindow;

import com.intellij.ide.dnd.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.awt.RelativeRectangle;
import mesfavoris.BookmarksException;
import mesfavoris.model.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles drag and drop operations for the bookmarks tree using IntelliJ Platform DnD system
 */
public class BookmarksTreeDnDHandler implements DnDSource, DnDTarget, Disposable {
    private static final Logger LOG = Logger.getInstance(BookmarksTreeDnDHandler.class);

    private final BookmarksTreeComponent tree;
    private final BookmarkDatabase bookmarkDatabase;

    public BookmarksTreeDnDHandler(BookmarksTreeComponent tree, BookmarkDatabase bookmarkDatabase) {
        this.tree = tree;
        this.bookmarkDatabase = bookmarkDatabase;

        // Register as source and target
        DnDManager.getInstance().registerSource(this, tree);
        DnDManager.getInstance().registerTarget(this, tree);
    }

    @Override
    public void dispose() {
        // Unregister from DnDManager
        DnDManager.getInstance().unregisterSource(this, tree);
        DnDManager.getInstance().unregisterTarget(this, tree);
    }
    
    // ========== DnDSource methods ==========
    
    @Override
    public boolean canStartDragging(DnDAction action, @NotNull Point dragOrigin) {
        TreePath[] selectionPaths = tree.getSelectionPaths();
        return selectionPaths != null && selectionPaths.length > 0;
    }
    
    @Override
    public DnDDragStartBean startDragging(DnDAction action, @NotNull Point dragOrigin) {
        TreePath[] selectionPaths = tree.getSelectionPaths();
        if (selectionPaths == null || selectionPaths.length == 0) {
            return null;
        }
        
        List<Bookmark> bookmarks = new ArrayList<>();
        for (TreePath path : selectionPaths) {
            Bookmark bookmark = tree.getBookmark(path);
            if (bookmark != null) {
                bookmarks.add(bookmark);
            }
        }
        
        if (bookmarks.isEmpty()) {
            return null;
        }
        
        return new DnDDragStartBean(bookmarks);
    }
    
    // ========== DnDTarget methods ==========
    
    @Override
    public boolean update(DnDEvent event) {
        event.setDropPossible(false);

        Object attached = event.getAttachedObject();
        if (!(attached instanceof List)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        List<Bookmark> bookmarks = (List<Bookmark>) attached;

        Point point = event.getPoint();
        DropLocation dropLocation = calculateDropLocation(point);

        if (dropLocation == null) {
            return false;
        }

        // Validate the drop
        BookmarksTree bookmarksTree = bookmarkDatabase.getBookmarksTree();
        BookmarkFolder parentFolder = dropLocation.getParentFolder(bookmarksTree);
        if (parentFolder == null) {
            return false;
        }

        boolean isValid = bookmarkDatabase.getBookmarksModificationValidator()
                .validateModification(bookmarksTree, parentFolder.getId()).isOk();

        if (isValid) {
            event.setDropPossible(true);

            // Set the drop highlight
            Rectangle bounds = dropLocation.bounds;
            if (dropLocation.type == DropType.INTO) {
                event.setHighlighting(new RelativeRectangle(tree, bounds), DnDEvent.DropTargetHighlightingType.RECTANGLE);
            } else {
                // Show line above or below
                Rectangle lineRect = new Rectangle(bounds.x,
                        dropLocation.type == DropType.BEFORE ? bounds.y : bounds.y + bounds.height - 1,
                        bounds.width, 2);
                event.setHighlighting(new RelativeRectangle(tree, lineRect), DnDEvent.DropTargetHighlightingType.FILLED_RECTANGLE);
            }
        }

        return true;
    }
    
    @Override
    public void drop(DnDEvent event) {
        Object attached = event.getAttachedObject();
        if (!(attached instanceof List)) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<Bookmark> bookmarks = (List<Bookmark>) attached;

        Point point = event.getPoint();
        DropLocation dropLocation = calculateDropLocation(point);

        if (dropLocation == null) {
            return;
        }

        performDrop(bookmarks, dropLocation);
    }
    
    @Override
    public void cleanUpOnLeave() {
        // Nothing to clean up
    }
    
    @Override
    public void updateDraggedImage(Image image, Point dropPoint, Point imageOffset) {
        // Use default image
    }
    
    // ========== Helper methods ==========
    
    private enum DropType {
        BEFORE,  // Drop before the target
        INTO,    // Drop into the target (for folders)
        AFTER    // Drop after the target
    }
    
    private static class DropLocation {
        final DropType type;
        final Bookmark targetBookmark;
        final Rectangle bounds;

        DropLocation(DropType type, Bookmark targetBookmark, Rectangle bounds) {
            this.type = type;
            this.targetBookmark = targetBookmark;
            this.bounds = bounds;
        }

        BookmarkFolder getParentFolder(BookmarksTree bookmarksTree) {
            if (type == DropType.INTO && targetBookmark instanceof BookmarkFolder) {
                return (BookmarkFolder) targetBookmark;
            } else {
                return bookmarksTree.getParentBookmark(targetBookmark.getId());
            }
        }
    }
    
    private DropLocation calculateDropLocation(Point point) {
        TreePath targetPath = tree.getClosestPathForLocation(point.x, point.y);
        if (targetPath == null) {
            return null;
        }

        Bookmark targetBookmark = tree.getBookmark(targetPath);
        if (targetBookmark == null) {
            return null;
        }

        Rectangle bounds = tree.getPathBounds(targetPath);
        if (bounds == null) {
            return null;
        }

        int relativeY = point.y - bounds.y;
        int height = bounds.height;

        if (targetBookmark instanceof BookmarkFolder) {
            // For folders: top 25% = before, middle 50% = into, bottom 25% = after
            if (relativeY < height * 0.25) {
                return new DropLocation(DropType.BEFORE, targetBookmark, bounds);
            } else if (relativeY > height * 0.75) {
                return new DropLocation(DropType.AFTER, targetBookmark, bounds);
            } else {
                return new DropLocation(DropType.INTO, targetBookmark, bounds);
            }
        } else {
            // For bookmarks: top 50% = before, bottom 50% = after
            if (relativeY < height * 0.5) {
                return new DropLocation(DropType.BEFORE, targetBookmark, bounds);
            } else {
                return new DropLocation(DropType.AFTER, targetBookmark, bounds);
            }
        }
    }
    
    private void performDrop(List<Bookmark> bookmarks, DropLocation dropLocation) {
        try {
            bookmarkDatabase.modify(bookmarksTreeModifier -> {
                List<Bookmark> existingBookmarks = new ArrayList<>();
                List<Bookmark> nonExistingBookmarks = new ArrayList<>();
                sortBookmarks(bookmarks, bookmarksTreeModifier.getCurrentTree(), existingBookmarks, nonExistingBookmarks);
                
                BookmarksTree bookmarksTree = bookmarksTreeModifier.getCurrentTree();
                BookmarkFolder parentFolder = dropLocation.getParentFolder(bookmarksTree);
                
                if (parentFolder == null) {
                    return;
                }
                
                List<BookmarkId> existingIds = existingBookmarks.stream()
                        .map(Bookmark::getId)
                        .collect(Collectors.toList());
                
                switch (dropLocation.type) {
                    case INTO:
                        // Drop into folder
                        bookmarksTreeModifier.move(existingIds, parentFolder.getId());
                        bookmarksTreeModifier.addBookmarks(parentFolder.getId(), nonExistingBookmarks);
                        break;
                        
                    case BEFORE:
                        // Drop before target
                        bookmarksTreeModifier.moveBefore(existingIds, parentFolder.getId(), dropLocation.targetBookmark.getId());
                        bookmarksTreeModifier.addBookmarksBefore(parentFolder.getId(), dropLocation.targetBookmark.getId(), nonExistingBookmarks);
                        break;
                        
                    case AFTER:
                        // Drop after target
                        bookmarksTreeModifier.moveAfter(existingIds, parentFolder.getId(), dropLocation.targetBookmark.getId());
                        bookmarksTreeModifier.addBookmarksAfter(parentFolder.getId(), dropLocation.targetBookmark.getId(), nonExistingBookmarks);
                        break;
                }
            });
        } catch (BookmarksException e) {
            LOG.error("Error during drag and drop", e);
        }
    }
    
    private void sortBookmarks(List<Bookmark> bookmarks, BookmarksTree bookmarksTree, 
                               List<Bookmark> existingBookmarks, List<Bookmark> nonExistingBookmarks) {
        for (Bookmark bookmark : bookmarks) {
            if (bookmarksTree.getBookmark(bookmark.getId()) != null) {
                existingBookmarks.add(bookmark);
            } else {
                nonExistingBookmarks.add(bookmark);
            }
        }
    }
}

