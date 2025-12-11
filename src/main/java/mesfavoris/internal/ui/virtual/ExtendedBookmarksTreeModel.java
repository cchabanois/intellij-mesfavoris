package mesfavoris.internal.ui.virtual;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.tree.BaseTreeModel;
import mesfavoris.internal.toolwindow.BookmarksTreeModel;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Add {@link VirtualBookmarkFolder} and {@link BookmarkLink} support to
 * {@link BookmarksTreeModel}
 *
 * @author cchabanois
 */
public class ExtendedBookmarksTreeModel extends BaseTreeModel<Object> implements Disposable {
    private final BookmarksTreeModel bookmarksTreeModel;
    private final List<VirtualBookmarkFolder> virtualBookmarkFolders;
    private final IVirtualBookmarkFolderListener virtualBookmarkFolderListener;
    private final TreeModelListener bookmarksTreeModelListener;

    public ExtendedBookmarksTreeModel(BookmarksTreeModel bookmarksTreeModel,
                                      List<VirtualBookmarkFolder> virtualBookmarkFolders,
                                      Disposable parentDisposable) {
        this.bookmarksTreeModel = bookmarksTreeModel;
        this.virtualBookmarkFolders = virtualBookmarkFolders;

        this.virtualBookmarkFolderListener = virtualBookmarkFolder ->
            ApplicationManager.getApplication().invokeLater(() -> {
                // Refresh the tree when virtual bookmark folder children change
                treeStructureChanged(null, null, null);
            });

        this.bookmarksTreeModelListener = new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                ExtendedBookmarksTreeModel.this.treeNodesChanged(e.getTreePath(), e.getChildIndices(), e.getChildren());
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                ExtendedBookmarksTreeModel.this.treeNodesInserted(e.getTreePath(), e.getChildIndices(), e.getChildren());
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
                ExtendedBookmarksTreeModel.this.treeNodesRemoved(e.getTreePath(), e.getChildIndices(), e.getChildren());
            }

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                ExtendedBookmarksTreeModel.this.treeStructureChanged(e.getTreePath(), e.getChildIndices(), e.getChildren());
            }
        };

        // Add listener to the underlying BookmarksTreeModel
        bookmarksTreeModel.addTreeModelListener(bookmarksTreeModelListener);

        // Add listeners to all virtual bookmark folders
        virtualBookmarkFolders.forEach(folder -> folder.addListener(virtualBookmarkFolderListener));

        // Register this component with the parent disposable
        Disposer.register(parentDisposable, this);
    }

    @Override
    public void dispose() {
        // Remove listener from the underlying BookmarksTreeModel
        bookmarksTreeModel.removeTreeModelListener(bookmarksTreeModelListener);

        // Remove listeners from all virtual bookmark folders
        virtualBookmarkFolders.forEach(folder -> folder.removeListener(virtualBookmarkFolderListener));
        super.dispose();
    }

    @Override
    public Object getRoot() {
        return bookmarksTreeModel.getRoot();
    }

    @Override
    public List<Object> getChildren(Object parent) {
        if (parent instanceof BookmarkFolder bookmarkFolder) {
            // Get regular children from the database
            List<Object> children = new ArrayList<>();
            List<Bookmark> bookmarkChildren = bookmarksTreeModel.getChildren(parent);
            children.addAll(bookmarkChildren);

            // Add virtual bookmark folders that have this folder as parent
            children.addAll(virtualBookmarkFolders.stream()
                .filter(virtualFolder -> virtualFolder.getParentId().equals(bookmarkFolder.getId()))
                .toList());

            return children;
        }

        if (parent instanceof VirtualBookmarkFolder virtualBookmarkFolder) {
            return new ArrayList<>(virtualBookmarkFolder.getChildren());
        }

        return Collections.emptyList();
    }

    @Override
    public boolean isLeaf(Object object) {
        if (object instanceof VirtualBookmarkFolder) {
            return false;
        }
        if (object instanceof BookmarkLink) {
            return true;
        }
        return bookmarksTreeModel.isLeaf(object);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // Delegate to bookmarksTreeModel for bookmark renaming
        bookmarksTreeModel.valueForPathChanged(path, newValue);
    }

}

