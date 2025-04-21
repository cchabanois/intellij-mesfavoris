package mesfavoris.internal.toolwindow;

import com.intellij.ide.DataManager;
import com.intellij.ide.dnd.aware.DnDAwareTree;
//import com.intellij.ide.favoritesTreeView.actions.DeleteFromFavoritesAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.ui.*;
import com.intellij.util.containers.Convertor;
import com.intellij.util.ui.JBUI;
import mesfavoris.BookmarksException;
import mesfavoris.commons.Adapters;
import mesfavoris.internal.bookmarktypes.BookmarkLabelProvider;
import mesfavoris.internal.ui.details.BookmarkDetailsPart;
import mesfavoris.internal.ui.details.CommentBookmarkDetailPart;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.service.BookmarksService;
import mesfavoris.texteditor.internal.TextEditorBookmarkLabelProvider;
import mesfavoris.ui.renderers.BookmarkFolderLabelProvider;
import mesfavoris.ui.renderers.BookmarksTreeCellRenderer;
import mesfavoris.url.internal.UrlBookmarkLabelProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;

public class MesFavorisPanel extends JPanel implements DataProvider, Disposable {
    private final Project project;
    private final DnDAwareTree tree;
    private final BookmarksService bookmarksService;

    public MesFavorisPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.bookmarksService = project.getService(BookmarksService.class);
        BookmarkDatabase bookmarkDatabase = bookmarksService.getBookmarkDatabase();
        tree = new DnDAwareTree(new BookmarksTreeModel(bookmarkDatabase));
        tree.setCellRenderer(new BookmarksTreeCellRenderer(project, bookmarkDatabase, bookmarksService.getBookmarksDirtyStateTracker(), new BookmarkLabelProvider(Arrays.asList(new UrlBookmarkLabelProvider(), new BookmarkFolderLabelProvider(), new TextEditorBookmarkLabelProvider()))));
        tree.setRootVisible(false);
        installTreeSpeedSearch();
        installDoubleClickListener();

        BookmarkDetailsPart bookmarkDetailsPart = new BookmarkDetailsPart(project, Arrays.asList(new CommentBookmarkDetailPart(project)));
        JComponent bookmarksDetailsComponent = bookmarkDetailsPart.createComponent();

        tree.getSelectionModel().addTreeSelectionListener(event -> {
            TreePath path = event.getPath();
            Bookmark bookmark = getBookmark(path);
            bookmarkDetailsPart.setBookmark(bookmark);

/*            SwingUtilities.invokeLater(() -> {
                if (!isDisposed()) {
                    updateOnSelectionChanged();
                    myNeedUpdateButtons = true;
                }
            }); */
        });

//        AnActionButton deleteActionButton = new DeleteFromFavoritesAction();
//        deleteActionButton.setShortcut(CustomShortcutSet.fromString("DELETE", "BACK_SPACE"));

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(tree)
                .initPosition()
                .disableAddAction().disableRemoveAction().disableDownAction().disableUpAction();
//                .addExtraAction(deleteActionButton);
        JPanel panel = decorator.createPanel();

        panel.setBorder(JBUI.Borders.empty());

        setBorder(JBUI.Borders.empty());

        JBSplitter splitter = new JBSplitter(true);
        splitter.setFirstComponent(panel);
        splitter.setSecondComponent(bookmarksDetailsComponent); // tabs.getComponent());

        add(splitter, BorderLayout.CENTER);
    }

    private void installDoubleClickListener() {
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent event) {
                TreePath clickPath = tree.getClosestPathForLocation(event.getX(), event.getY());
                if (clickPath == null) {
                    return false;
                }
                TreePath selectionPath = tree.getSelectionPath();
                if (!clickPath.equals(selectionPath)) return false;
                Object object = selectionPath.getLastPathComponent();
                Bookmark bookmark = Adapters.adapt(object, Bookmark.class);
                try {
                    ProgressIndicator progress = EmptyProgressIndicator.notNullize(ProgressIndicatorProvider.getGlobalProgressIndicator());
                    bookmarksService.gotoBookmark(bookmark.getId(), progress);
                } catch (BookmarksException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }.installOn(tree);
    }

    private void installTreeSpeedSearch() {
        Convertor<? super TreePath, String> TO_STRING = path -> path.getLastPathComponent().toString();
        TreeUIHelper.getInstance().installTreeSpeedSearch(tree, TO_STRING, true);
    }

    private Bookmark getBookmark(TreePath path) {
        Object object = path.getLastPathComponent();
        return Adapters.adapt(object, Bookmark.class);
    }

    @Override
    public void dispose() {
        DataManager.removeDataProvider(this);
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (PlatformDataKeys.SELECTED_ITEMS.is(dataId)) {
            return Arrays.stream(tree.getSelectionModel().getSelectionPaths()).map(this::getBookmark).toArray();
        }
        return null;
    }
}
