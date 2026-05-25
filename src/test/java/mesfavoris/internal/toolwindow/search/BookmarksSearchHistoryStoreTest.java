package mesfavoris.internal.toolwindow.search;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static mesfavoris.internal.toolwindow.search.BookmarksSearchHistoryStore.EntryType.BY_ID;
import static mesfavoris.internal.toolwindow.search.BookmarksSearchHistoryStore.EntryType.TEXT;
import static org.assertj.core.api.Assertions.assertThat;

public class BookmarksSearchHistoryStoreTest {
    private BookmarksSearchHistoryStore store;

    @Before
    public void setUp() {
        store = new BookmarksSearchHistoryStore();
    }

    @Test
    public void testAddTextSearch() {
        store.addTextSearch("first");
        store.addTextSearch("second");
        store.addTextSearch("third");

        List<BookmarksSearchHistoryStore.HistoryEntry> history = store.getHistory();
        assertThat(history).extracting(e -> e.label).containsExactly("third", "second", "first");
        assertThat(history).allMatch(e -> e.type == TEXT);
    }

    @Test
    public void testAddTextSearch_maxSize20() {
        for (int i = 1; i <= 25; i++) {
            store.addTextSearch("item" + i);
        }

        List<BookmarksSearchHistoryStore.HistoryEntry> history = store.getHistory();
        assertThat(history).hasSize(20);
        assertThat(history.get(0).label).isEqualTo("item25");
        assertThat(history).extracting(e -> e.label)
                .doesNotContain("item1", "item2", "item3", "item4", "item5");
    }

    @Test
    public void testAddTextSearch_duplicateMovedToFront() {
        store.addTextSearch("first");
        store.addTextSearch("second");
        store.addTextSearch("first");

        assertThat(store.getHistory()).extracting(e -> e.label).containsExactly("first", "second");
    }

    @Test
    public void testAddIdSearch_storesIds() {
        store.addIdSearch("Java bookmarks", List.of("b1", "b2"));

        List<BookmarksSearchHistoryStore.HistoryEntry> history = store.getHistory();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).type).isEqualTo(BY_ID);
        assertThat(history.get(0).label).isEqualTo("Java bookmarks");
        assertThat(history.get(0).ids).containsExactlyInAnyOrder("b1", "b2");
    }

    @Test
    public void testAddIdSearch_duplicateReplacesEntry() {
        store.addIdSearch("Java bookmarks", List.of("b1", "b2"));
        store.addIdSearch("Java bookmarks", List.of("b3"));

        List<BookmarksSearchHistoryStore.HistoryEntry> history = store.getHistory();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).ids).containsExactly("b3");
    }

    @Test
    public void testHistory_preservesInsertionOrder() {
        store.addTextSearch("grep error");
        store.addIdSearch("caffeine bookmarks", List.of("b1"));
        store.addTextSearch("async");

        List<BookmarksSearchHistoryStore.HistoryEntry> history = store.getHistory();
        assertThat(history).extracting(e -> e.type).containsExactly(TEXT, BY_ID, TEXT);
        assertThat(history).extracting(e -> e.label).containsExactly("async", "caffeine bookmarks", "grep error");
    }

    @Test
    public void testClearHistory() {
        store.addTextSearch("search1");
        store.addIdSearch("AI filter", List.of("b1"));

        store.clearHistory();

        assertThat(store.getHistory()).isEmpty();
    }
}
