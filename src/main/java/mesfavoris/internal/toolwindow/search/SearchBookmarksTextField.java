package mesfavoris.internal.toolwindow.search;

import com.intellij.openapi.project.Project;
import mesfavoris.model.BookmarkId;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchBookmarksTextField extends SearchHistoryTextField {

    private final Project project;
    private final BookmarksTreeFilter treeFilter;
    private final Runnable onFilterChanged;
    private final Map<String, Set<BookmarkId>> idFilterByLabel = new HashMap<>();

    public SearchBookmarksTextField(@NotNull Project project, @NotNull BookmarksTreeFilter treeFilter,
                                    @NotNull Runnable onFilterChanged) {
        super(project, "Search bookmarks");
        this.project = project;
        this.treeFilter = treeFilter;
        this.onFilterChanged = onFilterChanged;
        loadHistory();
        setupListener();
    }

    private void loadHistory() {
        BookmarksSearchHistoryStore store = project.getService(BookmarksSearchHistoryStore.class);
        List<BookmarksSearchHistoryStore.HistoryEntry> history = store.getHistory();

        for (BookmarksSearchHistoryStore.HistoryEntry entry : history) {
            if (entry.type == BookmarksSearchHistoryStore.EntryType.BY_ID && entry.ids != null) {
                Set<BookmarkId> ids = entry.ids.stream().map(BookmarkId::new).collect(Collectors.toSet());
                idFilterByLabel.put(entry.label, ids);
            }
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            setText(history.get(i).label);
            addCurrentTextToHistory();
        }
        setText("");
    }

    private void setupListener() {
        BookmarksSearchHistoryStore store = project.getService(BookmarksSearchHistoryStore.class);

        addSearchListener(new SearchListener() {
            @Override
            public void searchTextChanged(String searchText) {
                Set<BookmarkId> storedIds = idFilterByLabel.get(searchText);
                if (storedIds != null) {
                    applyIdFilter(storedIds, searchText);
                    return;
                }
                if (searchText.isEmpty() && treeFilter.isIdFiltering()) {
                    treeFilter.clear();
                    getTextEditor().setEditable(true);
                    onFilterChanged.run();
                    return;
                }
                if (treeFilter.isIdFiltering()) {
                    return;
                }
                treeFilter.setSearchText(searchText);
                onFilterChanged.run();
            }

            @Override
            public void searchPerformed(String searchText) {
                if (!searchText.isEmpty() && !treeFilter.isIdFiltering()) {
                    store.addTextSearch(searchText);
                }
            }
        });
    }

    public void showOnlyBookmarks(@NotNull Set<BookmarkId> ids, @NotNull String description) {
        String label = description.isBlank() ? "Filtered by AI" : "Filtered by AI: " + description;
        idFilterByLabel.put(label, ids);
        BookmarksSearchHistoryStore store = project.getService(BookmarksSearchHistoryStore.class);
        store.addIdSearch(label, ids.stream().map(BookmarkId::toString).toList());
        applyIdFilter(ids, label);
        addCurrentTextToHistory();
    }

    private void applyIdFilter(@NotNull Set<BookmarkId> ids, @NotNull String label) {
        treeFilter.setIdFilter(ids);
        getTextEditor().setEditable(false);
        if (!label.equals(getText())) {
            setText(label);
        }
        onFilterChanged.run();
    }
}
