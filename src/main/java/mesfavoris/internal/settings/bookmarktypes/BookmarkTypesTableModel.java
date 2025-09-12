package mesfavoris.internal.settings.bookmarktypes;

import mesfavoris.extensions.BookmarkTypeExtension;

import javax.swing.table.AbstractTableModel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Table model for bookmark types configuration
 */
public class BookmarkTypesTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {"", "Bookmark Type"};
    private static final Class<?>[] COLUMN_CLASSES = {Boolean.class, BookmarkTypeExtension.class};

    private final List<BookmarkTypeExtension> extensions;
    private Set<String> disabledBookmarkTypes;

    public BookmarkTypesTableModel(List<BookmarkTypeExtension> extensions, Set<String> disabledBookmarkTypes) {
        this.extensions = extensions;
        this.disabledBookmarkTypes = new HashSet<>(disabledBookmarkTypes);
    }
    
    @Override
    public int getRowCount() {
        return extensions.size();
    }
    
    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }
    
    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return COLUMN_CLASSES[columnIndex];
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0; // Only checkbox column is editable
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        BookmarkTypeExtension extension = extensions.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> !disabledBookmarkTypes.contains(extension.getName());
            case 1 -> extension;
            default -> null;
        };
    }
    
    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex == 0 && value instanceof Boolean enabled) {
            BookmarkTypeExtension extension = extensions.get(rowIndex);
            if (enabled) {
                disabledBookmarkTypes.remove(extension.getName());
            } else {
                disabledBookmarkTypes.add(extension.getName());
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
    
    public Set<String> getDisabledBookmarkTypes() {
        return new HashSet<>(disabledBookmarkTypes);
    }

    public void setDisabledBookmarkTypes(Set<String> disabledBookmarkTypes) {
        this.disabledBookmarkTypes = disabledBookmarkTypes;
        fireTableDataChanged();
    }

}
