package mesfavoris.internal.settings.placeholders;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import mesfavoris.placeholders.PathPlaceholder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel to configure placeholders
 */
public class PlaceholdersPanel extends JPanel {
    private PlaceholdersTableModel tableModel;
    private JBTable table;
    private JPanel mainPanel;

    public PlaceholdersPanel() {
        super(new BorderLayout());
        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        tableModel = new PlaceholdersTableModel();
        table = new JBTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Column sizes
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);

        // Add double-click listener to edit placeholders
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent event) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    editPlaceholder(selectedRow);
                    return true;
                }
                return false;
            }
        }.installOn(table);
    }

    private void layoutComponents() {
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction(button -> addPlaceholder())
            .setEditAction(button -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    editPlaceholder(selectedRow);
                }
            })
            .setRemoveAction(button -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    tableModel.removePlaceholder(selectedRow);
                }
            })
            .setEditActionUpdater(e -> table.getSelectedRow() >= 0)
            .setRemoveActionUpdater(e -> table.getSelectedRow() >= 0);

        mainPanel = decorator.createPanel();
        add(mainPanel, BorderLayout.CENTER);

        // Add explanatory label
        JLabel helpLabel = new JLabel("<html><body style='width: 400px'>" +
            "Placeholders allow you to define shortcuts for frequently used paths. " +
            "For example, a 'HOME' placeholder pointing to '/home/user' will allow you to use '${HOME}/documents' " +
            "in bookmarks." +
            "</body></html>");
        helpLabel.setBorder(JBUI.Borders.emptyBottom(10));
        add(helpLabel, BorderLayout.NORTH);
    }

    public void setPlaceholders(List<PathPlaceholder> placeholders) {
        tableModel.setItems(new ArrayList<>(placeholders));
    }

    public List<PathPlaceholder> getPlaceholders() {
        return new ArrayList<>(tableModel.getItems());
    }

    public boolean isModified(List<PathPlaceholder> originalPlaceholders) {
        List<PathPlaceholder> currentPlaceholders = getPlaceholders();
        if (currentPlaceholders.size() != originalPlaceholders.size()) {
            return true;
        }

        for (int i = 0; i < currentPlaceholders.size(); i++) {
            PathPlaceholder current = currentPlaceholders.get(i);
            PathPlaceholder original = originalPlaceholders.get(i);

            if (!current.equals(original)) {
                return true;
            }
        }

        return false;
    }

    private void addPlaceholder() {
        Project project = ProjectManager.getInstance().getDefaultProject();
        List<PathPlaceholder> existingPlaceholders = getPlaceholders();
        PlaceholderEditDialog dialog = new PlaceholderEditDialog(project, existingPlaceholders);

        if (dialog.showAndGet()) {
            PathPlaceholder newPlaceholder = dialog.getPlaceholder();
            if (newPlaceholder != null) {
                tableModel.addPlaceholder(newPlaceholder);
                int newRow = tableModel.getRowCount() - 1;
                table.getSelectionModel().setSelectionInterval(newRow, newRow);
            }
        }
    }

    private void editPlaceholder(int selectedRow) {
        PathPlaceholder currentPlaceholder = tableModel.getPlaceholder(selectedRow);
        if (currentPlaceholder != null) {
            Project project = ProjectManager.getInstance().getDefaultProject();
            List<PathPlaceholder> existingPlaceholders = getPlaceholders();
            PlaceholderEditDialog dialog = new PlaceholderEditDialog(project, currentPlaceholder, existingPlaceholders);

            if (dialog.showAndGet()) {
                PathPlaceholder updatedPlaceholder = dialog.getPlaceholder();
                if (updatedPlaceholder != null) {
                    tableModel.updatePlaceholder(selectedRow, updatedPlaceholder);
                }
            }
        }
    }

}
