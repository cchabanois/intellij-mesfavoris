package mesfavoris.internal.settings;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import mesfavoris.internal.settings.PlaceholdersTableModel.EditablePlaceholder;
import mesfavoris.placeholders.PathPlaceholder;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
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
        
        // Configure columns
        TableColumn pathColumn = table.getColumnModel().getColumn(1);
        pathColumn.setCellEditor(new PathCellEditor());

        // Column sizes
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);
    }

    private void layoutComponents() {
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction(button -> {
                tableModel.addPlaceholder();
                int newRow = tableModel.getRowCount() - 1;
                table.getSelectionModel().setSelectionInterval(newRow, newRow);
                table.editCellAt(newRow, 0);
            })
            .setRemoveAction(button -> {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    tableModel.removePlaceholder(selectedRow);
                }
            })
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
        List<EditablePlaceholder> editableList = new ArrayList<>();
        for (PathPlaceholder placeholder : placeholders) {
            editableList.add(new EditablePlaceholder(placeholder));
        }
        tableModel.setItems(editableList);
    }

    public List<PathPlaceholder> getPlaceholders() {
        List<PathPlaceholder> result = new ArrayList<>();
        for (EditablePlaceholder editable : tableModel.getItems()) {
            PathPlaceholder placeholder = editable.toPathPlaceholder();
            if (placeholder != null) {
                result.add(placeholder);
            }
        }
        return result;
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

    /**
     * Custom cell editor for paths with browse button
     */
    private static class PathCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final TextFieldWithBrowseButton component;
        private String currentValue;

        public PathCellEditor() {
            component = new TextFieldWithBrowseButton();

            // Add action listener for the browse button
            component.addActionListener(e -> {
                // Simple file chooser that works in both IDE and tests
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setDialogTitle("Select Folder");

                String currentPath = component.getText();
                if (!currentPath.isEmpty()) {
                    fileChooser.setCurrentDirectory(new java.io.File(currentPath));
                }

                int result = fileChooser.showOpenDialog(component);
                if (result == JFileChooser.APPROVE_OPTION) {
                    component.setText(fileChooser.getSelectedFile().getAbsolutePath());
                    stopCellEditing();
                }
            });
            
            component.getTextField().addActionListener(e -> stopCellEditing());
        }

        @Override
        public Object getCellEditorValue() {
            return component.getText();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentValue = value != null ? value.toString() : "";
            component.setText(currentValue);
            return component;
        }
    }
}
