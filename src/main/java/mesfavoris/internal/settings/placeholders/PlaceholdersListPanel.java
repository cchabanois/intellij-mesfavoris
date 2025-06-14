package mesfavoris.internal.settings.placeholders;

import com.intellij.openapi.project.Project;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import mesfavoris.bookmarktype.BookmarkDatabaseLabelProviderContext;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.internal.bookmarktypes.BookmarkLabelProvider;
import mesfavoris.internal.placeholders.PathPlaceholderResolver;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
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

/**
 * Panel with list to configure placeholders with statistics (for tool window)
 */
public class PlaceholdersListPanel extends JPanel {
    private CollectionListModel<PathPlaceholder> listModel;
    private JBList<PathPlaceholder> list;
    private JPanel mainPanel;
    private PathPlaceholderStats stats;
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

        stats = new PathPlaceholderStats(bookmarksService::getBookmarksTree, pathPropertyNames);

        PathPlaceholdersStore placeholdersStore = PathPlaceholdersStore.getInstance();
        pathPlaceholderResolver = new PathPlaceholderResolver(placeholdersStore);

        bookmarkLabelProvider = new BookmarkLabelProvider();

        listModel = new CollectionListModel<>();
        list = new JBList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom cell renderer to show placeholder name, usage count, and path
        list.setCellRenderer(new PlaceholderListCellRenderer());

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
            .setEditActionUpdater(e -> list.getSelectedIndex() >= 0)
            .setRemoveActionUpdater(e -> list.getSelectedIndex() >= 0);

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
                listModel.add(newPlaceholder);
                int newIndex = listModel.getSize() - 1;
                list.setSelectedIndex(newIndex);
            }
        }
    }

    private void editPlaceholder(int selectedIndex) {
        PathPlaceholder currentPlaceholder = listModel.getElementAt(selectedIndex);
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
        listModel.removeRow(selectedIndex);
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
        addButton.setEnabled(false); // Disabled for now
        buttonPanel.add(addButton, buttonGbc);

        buttonGbc.gridy++;
        JButton addAllButton = new JButton("Add All ▶▶");
        addAllButton.setEnabled(false); // Disabled for now
        buttonPanel.add(addAllButton, buttonGbc);

        buttonGbc.gridy++;
        JButton removeButton = new JButton("◀ Remove");
        removeButton.setEnabled(false); // Disabled for now
        buttonPanel.add(removeButton, buttonGbc);

        buttonGbc.gridy++;
        JButton removeAllButton = new JButton("◀◀ Remove All");
        removeAllButton.setEnabled(false); // Disabled for now
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

    /**
     * Custom cell renderer for placeholder list items
     */
    private class PlaceholderListCellRenderer extends ColoredListCellRenderer<PathPlaceholder> {
        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends PathPlaceholder> list, PathPlaceholder placeholder,
                                           int index, boolean selected, boolean hasFocus) {
            // Placeholder name
            append(placeholder.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);

            // Show usage statistics for the current project
            int usageCount = stats.getUsageCount(placeholder.getName());

            // Usage count in different color
            if (usageCount > 0) {
                append(" (" + usageCount + " matches)", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            } else {
                append(" (unused)", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }

            // Path
            append(" - " + placeholder.getPath().toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
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
