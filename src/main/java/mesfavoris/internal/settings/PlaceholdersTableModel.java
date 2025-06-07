package mesfavoris.internal.settings;

import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import mesfavoris.placeholders.PathPlaceholder;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;

/**
 * Table model to manage placeholders
 */
public class PlaceholdersTableModel extends ListTableModel<PlaceholdersTableModel.EditablePlaceholder> {

    /**
     * Editable wrapper for PathPlaceholder to use in table
     */
    public static class EditablePlaceholder {
        private String name;
        private String path;

        public EditablePlaceholder(String name, String path) {
            this.name = name;
            this.path = path;
        }

        public EditablePlaceholder(PathPlaceholder placeholder) {
            this.name = placeholder.getName();
            this.path = placeholder.getPath().toString();
        }

        public PathPlaceholder toPathPlaceholder() {
            if (name == null || name.trim().isEmpty() || path == null || path.trim().isEmpty()) {
                return null;
            }
            try {
                return new PathPlaceholder(name.trim(), Paths.get(path.trim()));
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static final ColumnInfo<EditablePlaceholder, String> NAME_COLUMN = new ColumnInfo<>("Name") {
        @Override
        public @Nullable String valueOf(EditablePlaceholder item) {
            return item.name;
        }

        @Override
        public void setValue(EditablePlaceholder item, String value) {
            item.name = value != null ? value.trim() : "";
        }

        @Override
        public boolean isCellEditable(EditablePlaceholder item) {
            return true;
        }

    };

    private static final ColumnInfo<EditablePlaceholder, String> PATH_COLUMN = new ColumnInfo<>("Path") {
        @Override
        public @Nullable String valueOf(EditablePlaceholder item) {
            return item.path;
        }

        @Override
        public void setValue(EditablePlaceholder item, String value) {
            item.path = value != null ? value.trim() : "";
        }

        @Override
        public boolean isCellEditable(EditablePlaceholder item) {
            return true;
        }

    };

    public PlaceholdersTableModel() {
        super(NAME_COLUMN, PATH_COLUMN);
    }

    public void addPlaceholder() {
        addRow(new EditablePlaceholder("", ""));
    }

    public void removePlaceholder(int index) {
        if (index >= 0 && index < getRowCount()) {
            removeRow(index);
        }
    }

    public EditablePlaceholder getPlaceholder(int index) {
        if (index >= 0 && index < getRowCount()) {
            return getItem(index);
        }
        return null;
    }
}
