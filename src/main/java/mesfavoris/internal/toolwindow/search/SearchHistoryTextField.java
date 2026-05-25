package mesfavoris.internal.toolwindow.search;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SearchTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryTextField extends SearchTextField implements Disposable {
    private static final int HISTORY_SIZE = 20;
    private final List<SearchListener> listeners = new ArrayList<>();

    public SearchHistoryTextField(@NotNull Project project, @NotNull String tooltipText) {
        super(true);

        setHistorySize(HISTORY_SIZE);
        getTextEditor().setToolTipText(tooltipText);

        getTextEditor().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String text = getText();
                if (!text.isEmpty()) {
                    addCurrentTextToHistory();
                    notifySearchPerformed(text);
                }
            }
        });

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

    public interface SearchListener {
        default void searchTextChanged(String searchText) {
        }

        default void searchPerformed(String searchText) {
        }
    }
}
