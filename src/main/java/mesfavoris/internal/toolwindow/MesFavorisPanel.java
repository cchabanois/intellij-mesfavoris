package mesfavoris.internal.toolwindow;

import com.intellij.ide.DataManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.*;
import com.intellij.util.containers.Convertor;
import com.intellij.util.ui.JBUI;
import mesfavoris.internal.actions.RemoteStoreActionGroup;
import mesfavoris.internal.markers.BookmarkWithMarkerLabelProvider;
import mesfavoris.internal.recent.RecentBookmarksVirtualFolder;
import mesfavoris.internal.toolwindow.search.BookmarksSearchHistoryStore;
import mesfavoris.internal.toolwindow.search.BookmarksSearchTextField;
import mesfavoris.internal.toolwindow.search.BookmarksTreeFilter;
import mesfavoris.internal.ui.details.BookmarkDetailsPart;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarksStoreExtension;
import mesfavoris.remote.RemoteBookmarksStoreExtensionManager;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.service.IBookmarksService;
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
    private final IBookmarksService bookmarksService;
    private final BookmarksTreeCellRenderer bookmarksTreeCellRenderer;
    private final BookmarkDetailsPart bookmarkDetailsPart;
    private final BookmarksTreeDnDHandler dndHandler;
    private final BookmarksSearchTextField searchTextField;
    private final BookmarksTreeFilter treeFilter;
    private final DefaultTreeExpander treeExpander;

    public MesFavorisPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.bookmarksService = project.getService(IBookmarksService.class);
        BookmarkDatabase bookmarkDatabase = bookmarksService.getBookmarkDatabase();

        // Create tree filter
        this.treeFilter = new BookmarksTreeFilter(bookmarkDatabase);

        BookmarkId rootId = bookmarkDatabase.getBookmarksTree().getRootFolder().getId();
        RecentBookmarksVirtualFolder recentBookmarksVirtualFolder = new RecentBookmarksVirtualFolder(project,
                bookmarkDatabase, bookmarksService.getRecentBookmarksProvider(), rootId, 20);

        tree = new BookmarksTreeComponent(bookmarkDatabase, treeFilter, List.of(recentBookmarksVirtualFolder),this);
        bookmarksTreeCellRenderer = new BookmarksTreeCellRenderer(project, bookmarkDatabase,
                project.getService(RemoteBookmarksStoreManager.class),
                bookmarksService.getBookmarksDirtyStateTracker(),
                new BookmarkWithMarkerLabelProvider(project, bookmarksService.getBookmarkLabelProvider()), this);
        tree.setCellRenderer(bookmarksTreeCellRenderer);
        tree.setEditable(true);

        // Create tree expander for collapse/expand all actions
        this.treeExpander = new DefaultTreeExpander(tree);

        // Create search text field
        this.searchTextField = new BookmarksSearchTextField(project);
        setupSearchTextField();

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
        JPanel treePanel = decorator.createPanel();
        treePanel.setBorder(JBUI.Borders.empty());

        // Create panel with search field and tree
        JPanel treeWithSearchPanel = new JPanel(new BorderLayout());
        treeWithSearchPanel.add(searchTextField, BorderLayout.NORTH);
        treeWithSearchPanel.add(treePanel, BorderLayout.CENTER);
        treeWithSearchPanel.setBorder(JBUI.Borders.empty());

        setBorder(JBUI.Borders.empty());

        JBSplitter splitter = new JBSplitter(true);
        splitter.setFirstComponent(treeWithSearchPanel);
        splitter.setSecondComponent(bookmarksDetailsComponent);

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
                    AnAction action = ActionManager.getInstance().getAction("mesfavoris.actions.GotoBookmarkAction");
                    AnActionEvent event = AnActionEvent.createEvent(context, null, ActionPlaces.UNKNOWN, ActionUiKind.NONE, mouseEvent);
                    ActionUtil.performAction(action, event);
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
                    .map(RemoteBookmarksStoreExtension::getAdditionalActions)
                    .orElse(List.of());
            RemoteStoreActionGroup storeGroup = new RemoteStoreActionGroup(store, additionalActions);
            popupMenu.add(storeGroup);
        }

        PopupHandler.installPopupMenu(tree, popupMenu, ActionPlaces.UNKNOWN);
    }

    private void setupSearchTextField() {
        // Load search history
        BookmarksSearchHistoryStore historyStore = project.getService(BookmarksSearchHistoryStore.class);
        List<String> history = historyStore.getSearchHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            searchTextField.setText(history.get(i));
            searchTextField.addCurrentTextToHistory();
        }
        searchTextField.setText("");

        // Add search listener
        searchTextField.addSearchListener(new BookmarksSearchTextField.SearchListener() {
            @Override
            public void searchTextChanged(String searchText) {
                treeFilter.setSearchText(searchText);

                // Refresh the tree model
                tree.refresh();

                // Expand all when filtering
                if (treeFilter.isFiltering()) {
                    expandAll();
                }
            }

            @Override
            public void searchPerformed(String searchText) {
                if (!searchText.isEmpty()) {
                    historyStore.addToHistory(searchText);
                }
            }
        });
    }

    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void installTreeSpeedSearch() {
        Convertor<? super TreePath, String> TO_STRING = path -> path.getLastPathComponent().toString();
        TreeUIHelper.getInstance().installTreeSpeedSearch(tree, TO_STRING, true);
    }

    @Override
    public void dispose() {
        searchTextField.dispose();
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
        if (PlatformDataKeys.TREE_EXPANDER.is(dataId)) {
            return treeExpander;
        }
        return null;
    }
}
