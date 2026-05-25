package mesfavoris.internal.toolwindow.search;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
    name = "BookmarksSearchHistory",
    storages = @Storage("mesfavoris.xml")
)
public class BookmarksSearchHistoryStore implements PersistentStateComponent<BookmarksSearchHistoryStore.State> {
    private static final int MAX_HISTORY_SIZE = 20;
    private State state = new State();

    public enum EntryType { TEXT, BY_ID }

    public static class HistoryEntry {
        public EntryType type;
        public String label;
        public List<String> ids;

        public HistoryEntry() {}

        public HistoryEntry(EntryType type, String label, List<String> ids) {
            this.type = type;
            this.label = label;
            this.ids = ids;
        }
    }

    public static class State {
        public List<HistoryEntry> history = new ArrayList<>();
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public void addTextSearch(@NotNull String label) {
        if (label.trim().isEmpty()) {
            return;
        }
        addEntry(new HistoryEntry(EntryType.TEXT, label, null));
    }

    public void addIdSearch(@NotNull String label, @NotNull List<String> ids) {
        addEntry(new HistoryEntry(EntryType.BY_ID, label, new ArrayList<>(ids)));
    }

    private void addEntry(HistoryEntry entry) {
        state.history.removeIf(e -> entry.label.equals(e.label));
        state.history.add(0, entry);
        if (state.history.size() > MAX_HISTORY_SIZE) {
            state.history = new ArrayList<>(state.history.subList(0, MAX_HISTORY_SIZE));
        }
    }

    public List<HistoryEntry> getHistory() {
        return new ArrayList<>(state.history);
    }

    public void clearHistory() {
        state.history.clear();
    }
}
