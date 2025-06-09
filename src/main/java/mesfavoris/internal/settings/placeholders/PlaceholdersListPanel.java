package mesfavoris.internal.settings.placeholders;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.path.PathBookmarkProperties;
import mesfavoris.placeholders.PathPlaceholder;
import mesfavoris.service.BookmarksService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Panel with list to configure placeholders with statistics (for tool window)
 */
public class PlaceholdersListPanel extends JPanel {
    private DefaultListModel<PathPlaceholder> listModel;
    private JBList<PathPlaceholder> list;
    private JPanel mainPanel;
    private PathPlaceholderStats stats;
    private final Project contextProject; // Project for statistics context

    public PlaceholdersListPanel() {
        this(null);
    }

    public PlaceholdersListPanel(@Nullable Project project) {
        super(new BorderLayout());
        this.contextProject = project;
        initComponents(project);
        layoutComponents();
    }

    private void initComponents(@Nullable Project project) {
        // Initialize stats with BookmarksService and file path property
        List<String> pathPropertyNames = Arrays.asList(PathBookmarkProperties.PROP_FILE_PATH);

        if (project != null && !project.isDisposed()) {
            try {
                BookmarksService bookmarksService = project.getService(BookmarksService.class);
                if (bookmarksService != null) {
                    stats = new PathPlaceholderStats(bookmarksService::getBookmarksTree, pathPropertyNames);
                } else {
                    stats = createFallbackStats(pathPropertyNames);
                }
            } catch (Exception e) {
                stats = createFallbackStats(pathPropertyNames);
            }
        } else {
            stats = createFallbackStats(pathPropertyNames);
        }

        listModel = new DefaultListModel<>();
        list = new JBList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom cell renderer to show placeholder name, usage count, and path
        list.setCellRenderer(new PlaceholderListCellRenderer());

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
        add(mainPanel, BorderLayout.CENTER);

        // Add explanatory label
        String helpText = "Placeholders allow you to define shortcuts for frequently used paths. " +
            "For example, a 'HOME' placeholder pointing to '/home/user' will allow you to use '${HOME}/documents' " +
            "in bookmarks.";

        if (contextProject != null) {
            helpText += " Usage statistics are shown for the current project.";
        }

        JLabel helpLabel = new JLabel("<html><body style='width: 400px'>" + helpText + "</body></html>");
        helpLabel.setBorder(JBUI.Borders.emptyBottom(10));
        add(helpLabel, BorderLayout.NORTH);
    }

    public void setPlaceholders(List<PathPlaceholder> placeholders) {
        listModel.clear();
        for (PathPlaceholder placeholder : placeholders) {
            listModel.addElement(placeholder);
        }
    }

    public List<PathPlaceholder> getPlaceholders() {
        List<PathPlaceholder> result = new ArrayList<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            result.add(listModel.getElementAt(i));
        }
        return result;
    }

    private void addPlaceholder() {
        Project project = getAvailableProject();
        List<PathPlaceholder> existingPlaceholders = getPlaceholders();
        PlaceholderEditDialog dialog = new PlaceholderEditDialog(project, existingPlaceholders);

        if (dialog.showAndGet()) {
            PathPlaceholder newPlaceholder = dialog.getPlaceholder();
            if (newPlaceholder != null) {
                listModel.addElement(newPlaceholder);
                int newIndex = listModel.getSize() - 1;
                list.setSelectedIndex(newIndex);
            }
        }
    }

    private void editPlaceholder(int selectedIndex) {
        PathPlaceholder currentPlaceholder = listModel.getElementAt(selectedIndex);
        if (currentPlaceholder != null) {
            Project project = getAvailableProject();
            List<PathPlaceholder> existingPlaceholders = getPlaceholders();
            PlaceholderEditDialog dialog = new PlaceholderEditDialog(project, currentPlaceholder, existingPlaceholders);

            if (dialog.showAndGet()) {
                PathPlaceholder updatedPlaceholder = dialog.getPlaceholder();
                if (updatedPlaceholder != null) {
                    listModel.setElementAt(updatedPlaceholder, selectedIndex);
                }
            }
        }
    }

    @Nullable
    private Project getAvailableProject() {
        // Try to get the first open project
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length > 0) {
            return openProjects[0];
        }
        return null;
    }

    private void removePlaceholder(int selectedIndex) {
        listModel.removeElementAt(selectedIndex);
    }

    private PathPlaceholderStats createFallbackStats(List<String> pathPropertyNames) {
        // Create stats with empty bookmarks tree for fallback
        BookmarkFolder rootFolder = new BookmarkFolder(new BookmarkId("root"), "Root");
        BookmarksTree emptyTree = new BookmarksTree(rootFolder);
        return new PathPlaceholderStats(() -> emptyTree, pathPropertyNames);
    }

    /**
     * Custom cell renderer for placeholder list items
     */
    private class PlaceholderListCellRenderer extends ColoredListCellRenderer<PathPlaceholder> {
        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends PathPlaceholder> list, PathPlaceholder placeholder,
                                           int index, boolean selected, boolean hasFocus) {
            if (placeholder != null) {
                // Placeholder name
                append(placeholder.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);

                // Show usage statistics only when we have a project context
                if (contextProject != null) {
                    int usageCount = stats.getUsageCount(placeholder.getName());

                    // Usage count in different color
                    if (usageCount > 0) {
                        append(" (" + usageCount + " matches)", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    } else {
                        append(" (unused)", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    }
                }

                // Path
                append(" - " + placeholder.getPath().toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
        }
    }

}
