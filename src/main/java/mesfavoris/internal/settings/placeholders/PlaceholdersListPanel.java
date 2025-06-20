package mesfavoris.internal.settings.placeholders;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import mesfavoris.BookmarksException;
import mesfavoris.bookmarktype.BookmarkDatabaseLabelProviderContext;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.internal.placeholders.PathPlaceholderResolver;
import mesfavoris.internal.service.operations.CollapseBookmarksOperation;
import mesfavoris.internal.service.operations.ExpandBookmarksOperation;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.path.PathBookmarkProperties;
import mesfavoris.placeholders.IPathPlaceholderResolver;
import mesfavoris.placeholders.PathPlaceholder;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static mesfavoris.placeholders.IPathPlaceholders.PLACEHOLDER_HOME_NAME;

/**
 * Panel with list to configure placeholders with statistics (for tool window)
 */
public class PlaceholdersListPanel extends JPanel {
    private CollectionListModel<PathPlaceholder> listModel;
    private JBList<PathPlaceholder> list;
    private JPanel mainPanel;
    private PathPlaceholderStats pathPlaceholderStats;
    private final Project project;
    private final BookmarksService bookmarksService;

    // Bookmark management components
    private JPanel bookmarkManagementPanel;
    private CollectionListModel<Bookmark> collapsableBookmarksModel;
    private CollectionListModel<Bookmark> collapsedBookmarksModel;
    private JBList<Bookmark> collapsableBookmarksList;
    private JBList<Bookmark> collapsedBookmarksList;
    private IPathPlaceholderResolver pathPlaceholderResolver;
    private IBookmarkLabelProvider bookmarkLabelProvider;

    public PlaceholdersListPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.bookmarksService = project.getService(BookmarksService.class);
        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        List<String> pathPropertyNames = Arrays.asList(PathBookmarkProperties.PROP_FILE_PATH);

        pathPlaceholderStats = new PathPlaceholderStats(bookmarksService::getBookmarksTree, pathPropertyNames);

        PathPlaceholdersStore placeholdersStore = PathPlaceholdersStore.getInstance();
        pathPlaceholderResolver = new PathPlaceholderResolver(placeholdersStore);

        bookmarkLabelProvider = bookmarksService.getBookmarkLabelProvider();

        listModel = new CollectionListModel<>();
        list = new JBList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom cell renderer to show placeholder name, usage count, and path
        list.setCellRenderer(new PathPlaceholderListCellRenderer(project));

        // Add selection listener to update bookmark lists
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateBookmarkLists();
            }
        });

        // Add double-click listener to edit placeholders
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent event) {
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex >= 0) {
                    editPlaceholder(selectedIndex);
                    return true;
                }
                return false;
            }
        }.installOn(list);

        // Initialize bookmark management components
        initBookmarkManagementComponents();
    }

    private void layoutComponents() {
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(list)
            .setAddAction(button -> addPlaceholder())
            .setEditAction(button -> {
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex >= 0) {
                    editPlaceholder(selectedIndex);
                }
            })
            .setRemoveAction(button -> {
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex >= 0) {
                    removePlaceholder(selectedIndex);
                }
            })
            .setEditActionUpdater(e -> {
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex < 0) {
                    return false;
                }
                PathPlaceholder selectedPlaceholder = listModel.getElementAt(selectedIndex);
                return !isUnmodifiable(selectedPlaceholder);
            })
            .setRemoveActionUpdater(e -> {
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex < 0) {
                    return false;
                }
                PathPlaceholder selectedPlaceholder = listModel.getElementAt(selectedIndex);
                return !isUnmodifiable(selectedPlaceholder);
            })
            .setMoveUpActionUpdater(e -> {
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex <= 0) {
                    return false;
                }
                PathPlaceholder selectedPlaceholder = listModel.getElementAt(selectedIndex);
                if (isUnmodifiable(selectedPlaceholder)) {
                    return false;
                }
                // Don't allow moving above HOME placeholder (which should be at index 0)
                if (selectedIndex == 1 && listModel.getSize() > 0) {
                    PathPlaceholder firstPlaceholder = listModel.getElementAt(0);
                    return !isUnmodifiable(firstPlaceholder);
                }
                return true;
            })
            .setMoveDownActionUpdater(e -> {
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex < 0 || selectedIndex >= listModel.getSize() - 1) {
                    return false;
                }
                PathPlaceholder selectedPlaceholder = listModel.getElementAt(selectedIndex);
                return !isUnmodifiable(selectedPlaceholder);
            });

        mainPanel = decorator.createPanel();

        // Create main content panel with 50/50 split using GridBagLayout
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints contentGbc = new GridBagConstraints();

        // Top half - Placeholders list (50%)
        contentGbc.gridx = 0;
        contentGbc.gridy = 0;
        contentGbc.weightx = 1.0;
        contentGbc.weighty = 0.5; // 50% of vertical space
        contentGbc.fill = GridBagConstraints.BOTH;
        contentGbc.insets = JBUI.insetsBottom(5); // Small gap between sections
        contentPanel.add(mainPanel, contentGbc);

        // Bottom half - Bookmark management (50%)
        contentGbc.gridy = 1;
        contentGbc.weighty = 0.5; // 50% of vertical space
        contentGbc.insets = JBUI.insetsTop(5); // Small gap between sections
        contentPanel.add(bookmarkManagementPanel, contentGbc);

        add(contentPanel, BorderLayout.CENTER);

        // Add explanatory label
        String helpText = "Placeholders allow you to define shortcuts for frequently used paths. " +
            "For example, a 'HOME' placeholder pointing to '/home/user' will allow you to use '${HOME}/documents' " +
            "in bookmarks. Usage statistics are shown for the current project.";

        JLabel helpLabel = new JLabel("<html><body style='width: 400px'>" + helpText + "</body></html>");
        helpLabel.setBorder(JBUI.Borders.emptyBottom(10));
        add(helpLabel, BorderLayout.NORTH);
    }

    public void setPlaceholders(List<PathPlaceholder> placeholders) {
        listModel.replaceAll(placeholders);
    }

    public List<PathPlaceholder> getPlaceholders() {
        return listModel.getItems();
    }

    private void addPlaceholder() {
        List<PathPlaceholder> existingPlaceholders = getPlaceholders();
        PlaceholderEditDialog dialog = new PlaceholderEditDialog(project, existingPlaceholders);

        if (dialog.showAndGet()) {
            PathPlaceholder newPlaceholder = dialog.getPlaceholder();
            if (newPlaceholder != null) {
                // Add new placeholder after HOME placeholder if it exists
                int insertIndex = listModel.getSize();
                if (listModel.getSize() > 0) {
                    PathPlaceholder firstPlaceholder = listModel.getElementAt(0);
                    if (isUnmodifiable(firstPlaceholder)) {
                        // HOME is at index 0, insert new placeholder at index 1 or later
                        insertIndex = Math.min(1, listModel.getSize());
                    }
                }

                if (insertIndex >= listModel.getSize()) {
                    listModel.add(newPlaceholder);
                    insertIndex = listModel.getSize() - 1;
                } else {
                    listModel.add(insertIndex, newPlaceholder);
                }

                list.setSelectedIndex(insertIndex);
            }
        }
    }

    private void editPlaceholder(int selectedIndex) {
        PathPlaceholder currentPlaceholder = listModel.getElementAt(selectedIndex);

        // Prevent editing of unmodifiable placeholders
        if (isUnmodifiable(currentPlaceholder)) {
            return;
        }

        List<PathPlaceholder> existingPlaceholders = getPlaceholders();
        PlaceholderEditDialog dialog = new PlaceholderEditDialog(project, currentPlaceholder, existingPlaceholders);

        if (dialog.showAndGet()) {
            PathPlaceholder updatedPlaceholder = dialog.getPlaceholder();
            if (updatedPlaceholder != null) {
                listModel.setElementAt(updatedPlaceholder, selectedIndex);
            }
        }
    }



    private void removePlaceholder(int selectedIndex) {
        PathPlaceholder currentPlaceholder = listModel.getElementAt(selectedIndex);

        // Prevent removal of unmodifiable placeholders
        if (isUnmodifiable(currentPlaceholder)) {
            return;
        }

        listModel.removeRow(selectedIndex);
    }

    /**
     * Check if a placeholder is unmodifiable (cannot be edited or removed)
     */
    private boolean isUnmodifiable(PathPlaceholder pathPlaceholder) {
        return PLACEHOLDER_HOME_NAME.equals(pathPlaceholder.getName());
    }



    private void initBookmarkManagementComponents() {
        bookmarkManagementPanel = new JPanel(new BorderLayout());
        bookmarkManagementPanel.setBorder(JBUI.Borders.emptyTop(10));

        // Set fixed preferred size to ensure consistent sizing
        bookmarkManagementPanel.setPreferredSize(JBUI.size(-1, 220)); // -1 for width, fixed height

        // Initialize list models
        collapsableBookmarksModel = new CollectionListModel<>();
        collapsedBookmarksModel = new CollectionListModel<>();

        // Initialize lists
        collapsableBookmarksList = new JBList<>(collapsableBookmarksModel);
        collapsedBookmarksList = new JBList<>(collapsedBookmarksModel);

        // Set cell renderers for bookmark lists with icons
        collapsableBookmarksList.setCellRenderer(new BookmarkListCellRenderer(project, bookmarksService, bookmarkLabelProvider));
        collapsedBookmarksList.setCellRenderer(new BookmarkListCellRenderer(project, bookmarksService, bookmarkLabelProvider));

        // Set fixed row height to ensure consistent sizing
        collapsableBookmarksList.setFixedCellHeight(20);
        collapsedBookmarksList.setFixedCellHeight(20);

        // Set visible row count to ensure consistent preferred size
        collapsableBookmarksList.setVisibleRowCount(8);
        collapsedBookmarksList.setVisibleRowCount(8);

        // Create the bookmark management UI
        createBookmarkManagementUI();
    }

    private void createBookmarkManagementUI() {
        // Use GridBagLayout for better control over resizing
        bookmarkManagementPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Left panel - Bookmarks with collapsable paths
        JPanel leftPanel = new JPanel(new BorderLayout());
        JLabel leftLabel = new JLabel("Bookmarks with collapsable paths");
        leftLabel.setBorder(JBUI.Borders.emptyBottom(5));
        leftPanel.add(leftLabel, BorderLayout.NORTH);
        JBScrollPane leftScrollPane = new JBScrollPane(collapsableBookmarksList);
        leftScrollPane.setMinimumSize(JBUI.size(200, 180));
        leftScrollPane.setPreferredSize(JBUI.size(250, 180));
        leftPanel.add(leftScrollPane, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.45; // 45% of horizontal space
        gbc.weighty = 1.0;  // Full vertical space
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = JBUI.insets(5);
        bookmarkManagementPanel.add(leftPanel, gbc);

        // Center panel - Transfer buttons
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints buttonGbc = new GridBagConstraints();
        buttonGbc.insets = JBUI.insets(5);
        buttonGbc.gridx = 0;
        buttonGbc.gridy = 0;
        buttonGbc.fill = GridBagConstraints.HORIZONTAL;

        JButton addButton = new JButton("Add ▶");
        addButton.addActionListener(e -> collapseSelectedBookmarks());
        buttonPanel.add(addButton, buttonGbc);

        buttonGbc.gridy++;
        JButton addAllButton = new JButton("Add All ▶▶");
        addAllButton.addActionListener(e -> collapseAllBookmarks());
        buttonPanel.add(addAllButton, buttonGbc);

        buttonGbc.gridy++;
        JButton removeButton = new JButton("◀ Remove");
        removeButton.addActionListener(e -> expandSelectedBookmarks());
        buttonPanel.add(removeButton, buttonGbc);

        buttonGbc.gridy++;
        JButton removeAllButton = new JButton("◀◀ Remove All");
        removeAllButton.addActionListener(e -> expandAllBookmarks());
        buttonPanel.add(removeAllButton, buttonGbc);

        gbc.gridx = 1;
        gbc.weightx = 0.1; // 10% of horizontal space for buttons
        gbc.fill = GridBagConstraints.VERTICAL;
        bookmarkManagementPanel.add(buttonPanel, gbc);

        // Right panel - Bookmarks with collapsed paths
        JPanel rightPanel = new JPanel(new BorderLayout());
        JLabel rightLabel = new JLabel("Bookmarks with collapsed paths");
        rightLabel.setBorder(JBUI.Borders.emptyBottom(5));
        rightPanel.add(rightLabel, BorderLayout.NORTH);
        JBScrollPane rightScrollPane = new JBScrollPane(collapsedBookmarksList);
        rightScrollPane.setMinimumSize(JBUI.size(200, 180));
        rightScrollPane.setPreferredSize(JBUI.size(250, 180));
        rightPanel.add(rightScrollPane, BorderLayout.CENTER);

        gbc.gridx = 2;
        gbc.weightx = 0.45; // 45% of horizontal space
        gbc.fill = GridBagConstraints.BOTH;
        bookmarkManagementPanel.add(rightPanel, gbc);

        // Always visible - lists will be empty when no placeholder is selected
        bookmarkManagementPanel.setVisible(true);
    }

    private void updateBookmarkLists() {
        PathPlaceholder selectedPlaceholder = list.getSelectedValue();

        // Clear existing lists
        collapsableBookmarksModel.removeAll();
        collapsedBookmarksModel.removeAll();

        if (selectedPlaceholder == null) {
            // No placeholder selected - lists remain empty but visible
            revalidate();
            repaint();
            return;
        }

        // Get bookmarks from the current project
        BookmarksTree bookmarksTree = bookmarksService.getBookmarksTree();
        populateBookmarkLists(bookmarksTree, selectedPlaceholder);

        // Refresh the UI
        revalidate();
        repaint();
    }

    private void populateBookmarkLists(BookmarksTree bookmarksTree, PathPlaceholder selectedPlaceholder) {
        for (Bookmark bookmark : bookmarksTree) {
            if (bookmark instanceof BookmarkFolder) {
                continue; // Skip folders
            }

            String filePath = bookmark.getPropertyValue(PathBookmarkProperties.PROP_FILE_PATH);
            if (filePath == null || filePath.trim().isEmpty()) {
                continue; // Skip bookmarks without file paths
            }

            // Check if bookmark already uses the selected placeholder
            String placeholderName = PathPlaceholderResolver.getPlaceholderName(filePath);
            if (selectedPlaceholder.getName().equals(placeholderName)) {
                collapsedBookmarksModel.add(bookmark);
            } else {
                // Check if the bookmark's path can be collapsed with the selected placeholder
                try {
                    Path expandedPath = pathPlaceholderResolver.expand(filePath);
                    if (expandedPath != null && expandedPath.startsWith(selectedPlaceholder.getPath())) {
                        collapsableBookmarksModel.add(bookmark);
                    }
                } catch (Exception e) {
                    // Skip bookmarks with invalid paths
                }
            }
        }
    }

    private void collapseSelectedBookmarks() {
        PathPlaceholder selectedPlaceholder = list.getSelectedValue();
        if (selectedPlaceholder == null) {
            return;
        }

        List<Bookmark> selectedBookmarks = collapsableBookmarksList.getSelectedValuesList();
        if (selectedBookmarks.isEmpty()) {
            return;
        }

        List<BookmarkId> bookmarkIds = selectedBookmarks.stream()
                .map(Bookmark::getId)
                .collect(Collectors.toList());

        collapseBookmarks(bookmarkIds, selectedPlaceholder.getName());
    }

    private void collapseAllBookmarks() {
        PathPlaceholder selectedPlaceholder = list.getSelectedValue();
        if (selectedPlaceholder == null) {
            return;
        }

        List<Bookmark> allBookmarks = collapsableBookmarksModel.getItems();
        if (allBookmarks.isEmpty()) {
            return;
        }

        List<BookmarkId> bookmarkIds = allBookmarks.stream()
                .map(Bookmark::getId)
                .collect(Collectors.toList());

        collapseBookmarks(bookmarkIds, selectedPlaceholder.getName());
    }

    private void collapseBookmarks(List<BookmarkId> bookmarkIds, String placeholderName) {
        try {
            CollapseBookmarksOperation operation = new CollapseBookmarksOperation(
                    bookmarksService.getBookmarkDatabase(),
                    PathPlaceholdersStore.getInstance(),
                    Arrays.asList(PathBookmarkProperties.PROP_FILE_PATH)
            );

            operation.collapse(bookmarkIds, placeholderName);

            // Update the bookmark lists to reflect the changes
            updateBookmarkLists();
        } catch (BookmarksException e) {
            // Show error notification
            String message = String.format("Failed to collapse bookmarks: %s", e.getMessage());
            showErrorNotification("Error", message);
        }
    }

    private void expandSelectedBookmarks() {
        PathPlaceholder selectedPlaceholder = list.getSelectedValue();
        if (selectedPlaceholder == null) {
            return;
        }

        List<Bookmark> selectedBookmarks = collapsedBookmarksList.getSelectedValuesList();
        if (selectedBookmarks.isEmpty()) {
            return;
        }

        List<BookmarkId> bookmarkIds = selectedBookmarks.stream()
                .map(Bookmark::getId)
                .collect(Collectors.toList());

        expandBookmarks(bookmarkIds);
    }

    private void expandAllBookmarks() {
        PathPlaceholder selectedPlaceholder = list.getSelectedValue();
        if (selectedPlaceholder == null) {
            return;
        }

        List<Bookmark> allBookmarks = collapsedBookmarksModel.getItems();
        if (allBookmarks.isEmpty()) {
            return;
        }

        List<BookmarkId> bookmarkIds = allBookmarks.stream()
                .map(Bookmark::getId)
                .collect(Collectors.toList());

        expandBookmarks(bookmarkIds);
    }

    private void expandBookmarks(List<BookmarkId> bookmarkIds) {
        try {
            ExpandBookmarksOperation operation = new ExpandBookmarksOperation(
                    bookmarksService.getBookmarkDatabase(),
                    PathPlaceholdersStore.getInstance(),
                    Arrays.asList(PathBookmarkProperties.PROP_FILE_PATH)
            );

            operation.expand(bookmarkIds);

            // Update the bookmark lists to reflect the changes
            updateBookmarkLists();
        } catch (BookmarksException e) {
            // Show error notification
            String message = String.format("Failed to expand bookmarks: %s", e.getMessage());
            showErrorNotification("Error", message);
        }
    }

    private void showErrorNotification(String title, String content) {
        Notification notification = new Notification("com.cchabanois.mesfavoris.errors", title, content, NotificationType.ERROR);
        notification.notify(project);
    }

    /**
     * Custom cell renderer for bookmark list items with icons
     */
    private static class BookmarkListCellRenderer extends ColoredListCellRenderer<Bookmark> {
        private final IBookmarkLabelProvider bookmarkLabelProvider;
        private final IBookmarkLabelProvider.Context context;

        public BookmarkListCellRenderer(@NotNull Project project, @NotNull BookmarksService bookmarksService, @NotNull IBookmarkLabelProvider bookmarkLabelProvider) {
            this.bookmarkLabelProvider = bookmarkLabelProvider;

            // Initialize Context once in constructor
            this.context = new BookmarkDatabaseLabelProviderContext(project, bookmarksService.getBookmarkDatabase());
        }

        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends Bookmark> list, Bookmark bookmark,
                                           int index, boolean selected, boolean hasFocus) {
            // Set bookmark icon
            Icon icon = bookmarkLabelProvider.getIcon(context, bookmark);
            setIcon(icon);

            // Bookmark name
            String name = bookmark.getPropertyValue(Bookmark.PROPERTY_NAME);
            if (name != null && !name.trim().isEmpty()) {
                append(name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            } else {
                append("Unnamed Bookmark", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }

            // File path
            String filePath = bookmark.getPropertyValue(PathBookmarkProperties.PROP_FILE_PATH);
            if (filePath != null) {
                append(" - " + filePath, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
        }
    }

}
