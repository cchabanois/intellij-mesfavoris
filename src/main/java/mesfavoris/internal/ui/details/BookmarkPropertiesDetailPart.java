package mesfavoris.internal.ui.details;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import mesfavoris.model.Bookmark;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BookmarkPropertiesDetailPart extends AbstractBookmarkDetailPart {
    private ListTableModel<Map.Entry<String, String>> listTableModel;

    public BookmarkPropertiesDetailPart(Project project) {
        super(project);
    }

    @Override
    public String getTitle() {
        return "Properties";
    }

    @Override
    public JComponent createComponent() {
        ColumnInfo<Map.Entry<String, String>, String> propertyNameColumn = getPropertyNameColumnInfo();
        ColumnInfo<Map.Entry<String, String>, String> propertyValueColumn = getPropertyValueColumnInfo();
        listTableModel =
                new ListTableModel<>(propertyNameColumn, propertyValueColumn);
        JBTable table = new JBTable(listTableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        return new JBScrollPane(table);
    }

    @Override
    public void setBookmark(Bookmark bookmark) {
        super.setBookmark(bookmark);
        List<Map.Entry<String, String>> items = new ArrayList<>(bookmark.getProperties().entrySet());
        listTableModel.setItems(items);
    }

    @Override
    protected void bookmarkModified(Bookmark oldBookmark, Bookmark newBookmark) {
        ApplicationManager.getApplication().invokeLater(() -> setBookmark(newBookmark));
    }

    private ColumnInfo<Map.Entry<String, String>, String> getPropertyNameColumnInfo() {
        return new ColumnInfo<>("Name") {
            @Override
            public String valueOf(Map.Entry<String, String> item) {
                return item.getKey();
            }

            @Override
            public boolean isCellEditable(Map.Entry<String, String> item) {
                return false;
            }

            @Override
            public Class<?> getColumnClass() {
                return String.class;
            }
        };
    }

    private ColumnInfo<Map.Entry<String, String>, String> getPropertyValueColumnInfo() {
        return new ColumnInfo<>("Value") {

            @Override
            public String valueOf(Map.Entry<String, String> item) {
                return item.getValue();
            }

            @Override
            public boolean isCellEditable(Map.Entry<String, String> item) {
                return false;
            }

            @Override
            public void setValue(Map.Entry<String, String> item, String value) {
            }

            @Override
            public Class<?> getColumnClass() {
                return String.class;
            }
        };
    }

    @Override
    public boolean canHandle(Bookmark bookmark) {
        return true;
    }
}
