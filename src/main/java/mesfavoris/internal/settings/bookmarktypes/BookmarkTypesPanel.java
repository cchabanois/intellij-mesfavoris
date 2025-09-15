package mesfavoris.internal.settings.bookmarktypes;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import mesfavoris.extensions.BookmarkTypeExtension;
import mesfavoris.extensions.BookmarkTypeExtensionManager;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Panel for managing bookmark types settings
 */
public class BookmarkTypesPanel extends JPanel {

    private final BookmarkTypesTableModel tableModel;
    private final JBTable table;

    public BookmarkTypesPanel(Set<String> disabledTypes) {
        super(new BorderLayout());

        List<BookmarkTypeExtension> extensions = loadBookmarkTypeExtensions();
        tableModel = new BookmarkTypesTableModel(extensions, disabledTypes);
        table = new JBTable(tableModel);

        setupTable();
        layoutComponents();
    }
    
    private List<BookmarkTypeExtension> loadBookmarkTypeExtensions() {
        BookmarkTypeExtensionManager manager = BookmarkTypeExtensionManager.getInstance();
        Collection<BookmarkTypeExtension> bookmarkTypes = manager.getAllBookmarkTypes();
        return new ArrayList<>(bookmarkTypes);
    }

    private void setupTable() {
        // Set row height to accommodate icon and description
        table.setRowHeight(48);

        // Set column widths
        table.getColumnModel().getColumn(0).setMaxWidth(50); // checkbox column
        table.getColumnModel().getColumn(0).setMinWidth(50);

        // Custom renderer for the name/description column (with icon)
        table.getColumnModel().getColumn(1).setCellRenderer(new BookmarkTypeNameRenderer());

        // Disable reordering
        table.getTableHeader().setReorderingAllowed(false);
    }
    
    private void layoutComponents() {
        setBorder(JBUI.Borders.empty(10));
        
        JBScrollPane scrollPane = new JBScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        
        JLabel descriptionLabel = new JLabel("<html><i>Configure which bookmark types are enabled.</i></html>");
        descriptionLabel.setBorder(JBUI.Borders.emptyBottom(10));
        add(descriptionLabel, BorderLayout.NORTH);
    }
    

    
    /**
     * Get disabled bookmark types
     * @return set of disabled bookmark type names
     */
    public Set<String> getDisabledBookmarkTypes() {
        return tableModel.getDisabledBookmarkTypes();
    }

    /**
     * Set disabled bookmark types
     * @param disabledTypes set of bookmark type names to disable
     */
    public void setDisabledBookmarkTypes(Set<String> disabledTypes) {
        tableModel.setDisabledBookmarkTypes(disabledTypes);
    }

    /**
     * Get table model for testing purposes
     * @return the table model
     */
    public BookmarkTypesTableModel getTableModel() {
        return tableModel;
    }

    /**
     * Custom renderer for bookmark type name, icon and description
     */
    private static class BookmarkTypeNameRenderer extends JPanel implements TableCellRenderer {
        private final JLabel nameLabel;
        private final JLabel descriptionLabel;

        public BookmarkTypeNameRenderer() {
            super(new BorderLayout());
            setBorder(JBUI.Borders.empty(4, 8));

            nameLabel = new JLabel();
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));

            descriptionLabel = new JLabel();
            descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.PLAIN,
                descriptionLabel.getFont().getSize() - 1));
            descriptionLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());

            add(nameLabel, BorderLayout.NORTH);
            add(descriptionLabel, BorderLayout.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            if (value instanceof BookmarkTypeExtension extension) {
                // Set icon and text for the name label
                nameLabel.setIcon(extension.getIcon());
                nameLabel.setText(extension.getName());
                descriptionLabel.setText(extension.getDescription());
            }

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setOpaque(true);
            } else {
                setBackground(table.getBackground());
                setOpaque(false);
            }

            return this;
        }
    }
}
