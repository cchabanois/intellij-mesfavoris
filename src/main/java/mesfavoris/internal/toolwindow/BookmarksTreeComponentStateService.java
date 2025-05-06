package mesfavoris.internal.toolwindow;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import mesfavoris.model.BookmarkId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@State(
        name = "BookmarksTreeComponentState",
        storages = @Storage("bookmarksTreeComponentState.xml")
)
public class BookmarksTreeComponentStateService implements PersistentStateComponent<BookmarksTreeComponentStateService.BookmarksTreeComponentState> {

    public static class BookmarksTreeComponentState {
        public List<String> expandedPaths = new ArrayList<>();

        public BookmarksTreeComponentState() {
        }
    }

    private BookmarksTreeComponentState state = new BookmarksTreeComponentState();

    @Override
    public @Nullable BookmarksTreeComponentState getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull BookmarksTreeComponentState state) {
        this.state = state;
    }

    public void installTreeExpansionPersistance(BookmarksTreeComponent tree) {
        TreeExpansionListener expansionListener = new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                saveExpansionState(tree);
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                saveExpansionState(tree);
            }
        };
        tree.addTreeExpansionListener(expansionListener);
        restoreExpansionState(tree);
    }

    private void saveExpansionState(BookmarksTreeComponent tree) {
        Set<BookmarkId> bookmarkIds = tree.getExpandedPaths().stream().map(path -> tree.getBookmark(path).getId()).collect(Collectors.toSet());
        this.state.expandedPaths = bookmarkIds.stream().map(BookmarkId::toString).toList();
    }

    private void restoreExpansionState(BookmarksTreeComponent tree) {
        Set<BookmarkId> bookmarkIds = this.state.expandedPaths.stream().map(BookmarkId::new).collect(Collectors.toSet());
        Set<TreePath> treePaths = bookmarkIds.stream().map(tree::getTreePathForBookmark).collect(Collectors.toSet());
        for (TreePath treePath : treePaths) {
            tree.expandPath(treePath);
        }
    }

}
