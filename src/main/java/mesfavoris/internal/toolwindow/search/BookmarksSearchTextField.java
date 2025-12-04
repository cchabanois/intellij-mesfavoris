package mesfavoris.internal.toolwindow.search;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SearchTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Search text field for bookmarks with history support
 */
public class BookmarksSearchTextField extends SearchTextField implements Disposable {
    private static final int HISTORY_SIZE = 20;
    private final Project project;
    private final List<SearchListener> listeners = new ArrayList<>();

    public BookmarksSearchTextField(@NotNull Project project) {
        super(true);
        this.project = project;

        setHistorySize(HISTORY_SIZE);
        getTextEditor().setToolTipText("Search bookmarks");

        // Add key listener for Enter key
        getTextEditor().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String text = getText();
                    if (!text.isEmpty()) {
                        addCurrentTextToHistory();
                        notifySearchPerformed(text);
                    }
                }
            }
        });

        // Add document listener for real-time search
        addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                notifySearchTextChanged(getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifySearchTextChanged(getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifySearchTextChanged(getText());
            }
        });

        setBorder(JBUI.Borders.empty(4));
    }

    public void addSearchListener(SearchListener listener) {
        listeners.add(listener);
    }

    public void removeSearchListener(SearchListener listener) {
        listeners.remove(listener);
    }

    private void notifySearchTextChanged(String searchText) {
        for (SearchListener listener : listeners) {
            listener.searchTextChanged(searchText);
        }
    }

    private void notifySearchPerformed(String searchText) {
        for (SearchListener listener : listeners) {
            listener.searchPerformed(searchText);
        }
    }

    @Override
    public void dispose() {
        listeners.clear();
    }

    /**
     * Listener for search events
     */
    public interface SearchListener {
        /**
         * Called when search text changes (real-time)
         */
        default void searchTextChanged(String searchText) {
        }

        /**
         * Called when search is performed (Enter key)
         */
        default void searchPerformed(String searchText) {
        }
    }
}

