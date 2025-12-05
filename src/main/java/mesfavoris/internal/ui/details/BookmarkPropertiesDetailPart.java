package mesfavoris.internal.ui.details;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import mesfavoris.BookmarksException;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.service.BookmarksService;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookmarkPropertiesDetailPart extends AbstractBookmarkDetailPart {
    private ListTableModel<Map.Entry<String, String>> listTableModel;
    private JBTable table;

    public BookmarkPropertiesDetailPart(Project project) {
        this(project, project.getService(BookmarksService.class).getBookmarkDatabase());
    }

    public BookmarkPropertiesDetailPart(Project project, BookmarkDatabase  bookmarkDatabase) {
        super(project, bookmarkDatabase);
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
        table = new JBTable(listTableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        return new JBScrollPane(table);
    }

    @Override
    public void setBookmark(Bookmark bookmark) {
        super.setBookmark(bookmark);
        updateTableItems();
        table.setEnabled(this.bookmark != null && bookmarkDatabase.getBookmarksModificationValidator()
                .validateModification(bookmarkDatabase.getBookmarksTree(), bookmark.getId())
                .isOk());
    }

    private void updateTableItems() {
        List<Map.Entry<String, String>> items = bookmark == null ? List.of() : new ArrayList<>(bookmark.getProperties().entrySet());
        listTableModel.setItems(items);
    }

    @Override
    protected void bookmarkModified(Bookmark oldBookmark, Bookmark newBookmark) {
        updateTableItems();
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
                return true;
            }

            @Override
            public void setValue(Map.Entry<String, String> item, String value) {
                BookmarksService bookmarksService = project.getService(BookmarksService.class);
                Map<String, String> newProperties = new HashMap<>(bookmark.getProperties());
                newProperties.put(item.getKey(), value);
                try {
                    bookmarksService.setBookmarkProperties(bookmark.getId(), newProperties);
                } catch (BookmarksException e) {
                    // ignore
                }
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
