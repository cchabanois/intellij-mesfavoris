package mesfavoris.internal.toolwindow;

import com.intellij.ide.dnd.aware.DnDAwareTree;
//import com.intellij.ide.favoritesTreeView.actions.DeleteFromFavoritesAction;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.util.ui.JBUI;
import mesfavoris.BookmarksException;
import mesfavoris.commons.Adapters;
import mesfavoris.internal.bookmarktypes.BookmarkLabelProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.service.BookmarksService;
import mesfavoris.texteditor.internal.TextEditorBookmarkLabelProvider;
import mesfavoris.ui.renderers.BookmarkFolderLabelProvider;
import mesfavoris.ui.renderers.BookmarksTreeCellRenderer;
import mesfavoris.url.internal.UrlBookmarkLabelProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;

public class MesFavorisPanel extends JPanel {

    private final Project project;
    private final DnDAwareTree tree;

    public MesFavorisPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        BookmarksService bookmarksService = project.getService(BookmarksService.class);
        BookmarkDatabase bookmarkDatabase = bookmarksService.getBookmarkDatabase();
        tree = new DnDAwareTree(new BookmarksTreeModel(bookmarkDatabase));
        tree.setCellRenderer(new BookmarksTreeCellRenderer(project, bookmarkDatabase, new BookmarkLabelProvider(Arrays.asList(new UrlBookmarkLabelProvider(), new BookmarkFolderLabelProvider(), new TextEditorBookmarkLabelProvider()))));
        tree.setRootVisible(false);
        new TreeSpeedSearch(tree);


        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent event) {
                TreePath clickPath = tree.getClosestPathForLocation(event.getX(), event.getY());
                if (clickPath == null) {
                    return false;
                }
                TreePath selectionPath = tree.getSelectionPath();
                if (selectionPath == null || !clickPath.equals(selectionPath)) return false;
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


//        AnActionButton deleteActionButton = new DeleteFromFavoritesAction();
//        deleteActionButton.setShortcut(CustomShortcutSet.fromString("DELETE", "BACK_SPACE"));

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(tree)
                .initPosition()
                .disableAddAction().disableRemoveAction().disableDownAction().disableUpAction();
//                .addExtraAction(deleteActionButton);
        JPanel panel = decorator.createPanel();

        panel.setBorder(JBUI.Borders.empty());
        add(panel, BorderLayout.CENTER);
        setBorder(JBUI.Borders.empty());
    }

}
