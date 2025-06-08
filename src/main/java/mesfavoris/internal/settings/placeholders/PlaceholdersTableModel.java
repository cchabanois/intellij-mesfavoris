package mesfavoris.internal.settings.placeholders;

import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import mesfavoris.placeholders.PathPlaceholder;
import org.jetbrains.annotations.Nullable;

/**
 * Table model to manage placeholders
 */
public class PlaceholdersTableModel extends ListTableModel<PathPlaceholder> {

    private static final ColumnInfo<PathPlaceholder, String> NAME_COLUMN = new ColumnInfo<>("Name") {
        @Override
        public @Nullable String valueOf(PathPlaceholder item) {
            return item.getName();
        }

        @Override
        public void setValue(PathPlaceholder item, String value) {
            // Not used since table is not editable
        }
    };

    private static final ColumnInfo<PathPlaceholder, String> PATH_COLUMN = new ColumnInfo<>("Path") {
        @Override
        public String valueOf(PathPlaceholder item) {
            return item.getPath().toString();
        }

        @Override
        public void setValue(PathPlaceholder item, String value) {
            // Not used since table is not editable
        }
    };

    public PlaceholdersTableModel() {
        super(NAME_COLUMN, PATH_COLUMN);
    }

    public void removePlaceholder(int index) {
        if (index >= 0 && index < getRowCount()) {
            removeRow(index);
        }
    }

    public PathPlaceholder getPlaceholder(int index) {
        if (index >= 0 && index < getRowCount()) {
            return getItem(index);
        }
        return null;
    }

    public void updatePlaceholder(int index, PathPlaceholder placeholder) {
        if (index >= 0 && index < getRowCount()) {
            setItem(index, placeholder);
        }
    }

    public void addPlaceholder(PathPlaceholder placeholder) {
        addRow(placeholder);
    }
}
