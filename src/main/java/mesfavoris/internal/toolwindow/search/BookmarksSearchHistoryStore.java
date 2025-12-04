package mesfavoris.internal.toolwindow.search;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent storage for bookmarks search history
 */
@State(
    name = "BookmarksSearchHistory",
    storages = @Storage("bookmarksSearchHistory.xml")
)
public class BookmarksSearchHistoryStore implements PersistentStateComponent<BookmarksSearchHistoryStore.State> {
    private static final int MAX_HISTORY_SIZE = 20;
    private State state = new State();

    public static class State {
        public List<String> searchHistory = new ArrayList<>();

        public State() {
        }
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public List<String> getSearchHistory() {
        return new ArrayList<>(state.searchHistory);
    }

    public void addToHistory(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return;
        }

        // Remove if already exists
        state.searchHistory.remove(searchText);

        // Add to beginning
        state.searchHistory.add(0, searchText);

        // Limit size
        if (state.searchHistory.size() > MAX_HISTORY_SIZE) {
            state.searchHistory = new ArrayList<>(state.searchHistory.subList(0, MAX_HISTORY_SIZE));
        }
    }

    public void clearHistory() {
        state.searchHistory.clear();
    }
}

