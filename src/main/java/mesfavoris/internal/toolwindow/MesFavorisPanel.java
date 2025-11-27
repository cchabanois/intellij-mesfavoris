package mesfavoris.internal.toolwindow;

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.*;
import com.intellij.util.containers.Convertor;
import com.intellij.util.ui.JBUI;
import mesfavoris.internal.actions.RemoteStoreActionGroup;
import mesfavoris.internal.markers.BookmarkWithMarkerLabelProvider;
import mesfavoris.internal.ui.details.BookmarkDetailsPart;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarksStoreExtensionManager;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.service.BookmarksService;
import mesfavoris.ui.renderers.BookmarksTreeCellRenderer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;


public class MesFavorisPanel extends JPanel implements DataProvider, Disposable {
    private final Project project;
    private final BookmarksTreeComponent tree;
    private final BookmarksService bookmarksService;
    private final BookmarksTreeCellRenderer bookmarksTreeCellRenderer;
    private final BookmarkDetailsPart bookmarkDetailsPart;
    private final BookmarksTreeDnDHandler dndHandler;

    public MesFavorisPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.bookmarksService = project.getService(BookmarksService.class);
        BookmarkDatabase bookmarkDatabase = bookmarksService.getBookmarkDatabase();
        tree = new BookmarksTreeComponent(bookmarkDatabase, this);
        bookmarksTreeCellRenderer = new BookmarksTreeCellRenderer(project, bookmarkDatabase,
                project.getService(RemoteBookmarksStoreManager.class),
                bookmarksService.getBookmarksDirtyStateTracker(),
                new BookmarkWithMarkerLabelProvider(project, bookmarksService.getBookmarkLabelProvider()), this);
        tree.setCellRenderer(bookmarksTreeCellRenderer);
        tree.setEditable(true);
        installTreeSpeedSearch();
        installDoubleClickListener();
        installPopupMenu();

        // Setup drag and drop
        dndHandler = new BookmarksTreeDnDHandler(tree, bookmarkDatabase);
        Disposer.register(this, dndHandler);

        bookmarkDetailsPart = new BookmarkDetailsPart(project, this);
        bookmarkDetailsPart.init();
        JComponent bookmarksDetailsComponent = bookmarkDetailsPart.createComponent();

        tree.getSelectionModel().addTreeSelectionListener(event -> {
            TreePath path = event.getPath();
            Bookmark bookmark = tree.getBookmark(path);
            bookmarkDetailsPart.setBookmark(bookmark);
        });
        BookmarksTreeComponentStateService bookmarksTreeComponentStateService = project.getService(BookmarksTreeComponentStateService.class);
        bookmarksTreeComponentStateService.installTreeExpansionPersistance(tree);

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(tree)
                .initPosition()
                .disableAddAction().disableRemoveAction().disableDownAction().disableUpAction();
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
            protected boolean onDoubleClick(@NotNull MouseEvent mouseEvent) {
                TreePath clickPath = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
                if (clickPath == null) {
                    return false;
                }
                TreePath selectionPath = tree.getSelectionPath();
                if (!clickPath.equals(selectionPath)) return false;

                Bookmark bookmark = tree.getBookmark(selectionPath);
                if (bookmark instanceof BookmarkFolder) {
                    return false;
                } else {
                    DataContext context = DataManager.getInstance().getDataContext(MesFavorisPanel.this);

                    AnActionEvent event = AnActionEvent.createEvent(context, null, ActionPlaces.UNKNOWN, ActionUiKind.NONE, mouseEvent);

                    ActionManager.getInstance()
                            .getAction("mesfavoris.actions.GotoBookmarkAction")
                            .actionPerformed(event);
                    return true;
                }
            }
        }.installOn(tree);
    }

    private void installPopupMenu() {
        DefaultActionGroup popupMenu = (DefaultActionGroup)ActionManager.getInstance().getAction("mesfavoris.PopupMenu");

        // Add remote store menus
        RemoteBookmarksStoreExtensionManager manager = project.getService(RemoteBookmarksStoreExtensionManager.class);
        List<IRemoteBookmarksStore> stores = manager.getStores();
        for (IRemoteBookmarksStore store : stores) {
            List<AnAction> additionalActions = manager.getExtension(store.getDescriptor().id())
                    .map(extension -> extension.getAdditionalActions())
                    .orElse(List.of());
            RemoteStoreActionGroup storeGroup = new RemoteStoreActionGroup(store, additionalActions);
            popupMenu.add(storeGroup);
        }

        PopupHandler.installPopupMenu(tree, popupMenu, ActionPlaces.UNKNOWN);
    }

    private void installTreeSpeedSearch() {
        Convertor<? super TreePath, String> TO_STRING = path -> path.getLastPathComponent().toString();
        TreeUIHelper.getInstance().installTreeSpeedSearch(tree, TO_STRING, true);
    }

    @Override
    public void dispose() {
        DataManager.removeDataProvider(this);
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (PlatformDataKeys.SELECTED_ITEM.is(dataId)) {
            TreePath selectionPath = tree.getSelectionModel().getSelectionPath();
            return selectionPath != null ? tree.getBookmark(selectionPath) : null;
        }
        if (PlatformDataKeys.SELECTED_ITEMS.is(dataId)) {
            return Arrays.stream(tree.getSelectionModel().getSelectionPaths()).map(tree::getBookmark).toArray();
        }
        return null;
    }
}
