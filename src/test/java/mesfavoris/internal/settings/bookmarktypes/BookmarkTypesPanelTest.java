package mesfavoris.internal.settings.bookmarktypes;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.table.JBTable;
import mesfavoris.extensions.BookmarkTypeExtension;
import mesfavoris.extensions.BookmarkTypeExtensionManager;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for BookmarkTypesPanel
 */
public class BookmarkTypesPanelTest extends BasePlatformTestCase {

    private BookmarkTypesPanel panel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        panel = new BookmarkTypesPanel(Set.of()); // No disabled types for testing
    }

    public void testCellRenderer() {
        // Given
        JBTable table = findTableInPanel(panel);
        TableCellRenderer renderer = table.getColumnModel().getColumn(1).getCellRenderer();
        BookmarkTypeExtension extension = BookmarkTypeExtensionManager.getInstance()
                .getBookmarkType("textEditor").get();

        // When
        Component renderedComponent = renderer.getTableCellRendererComponent(
                table, extension, false, false, 0, 1);

        // Then - verify the renderer uses the extension's name, icon and description
        JPanel renderedPanel = (JPanel) renderedComponent;
        BorderLayout layout = (BorderLayout) renderedPanel.getLayout();

        JLabel nameLabel = (JLabel) layout.getLayoutComponent(BorderLayout.NORTH);
        JLabel descriptionLabel = (JLabel) layout.getLayoutComponent(BorderLayout.CENTER);

        assertThat(nameLabel.getText()).isEqualTo("textEditor");
        assertThat(nameLabel.getIcon()).isEqualTo(extension.getIcon());
        assertThat(descriptionLabel.getText())
                .isEqualTo("Bookmarks for text files with line and content information");
    }

    private JBTable findTableInPanel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JBTable) {
                return (JBTable) component;
            }
            if (component instanceof JScrollPane scrollPane) {
                Component viewport = scrollPane.getViewport().getView();
                if (viewport instanceof JBTable) {
                    return (JBTable) viewport;
                }
            }
            if (component instanceof Container) {
                JBTable found = findTableInPanel((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
