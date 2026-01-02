package mesfavoris.internal.markers;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.model.BookmarkId;
import org.jdom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class BookmarksMarkersStoreTest extends BasePlatformTestCase {

    private BookmarksMarkersStore bookmarksMarkersStore;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        bookmarksMarkersStore = new BookmarksMarkersStore();
    }

    public void testPutAndGetByBookmarkId() {
        // Given
        VirtualFile file = myFixture.getTempDirFixture().createFile("test.txt");
        BookmarkId bookmarkId = new BookmarkId("bookmark1");
        Map<String, String> attributes = new HashMap<>();
        attributes.put(BookmarkMarker.LINE_NUMBER, "10");
        BookmarkMarker marker = new BookmarkMarker(file, bookmarkId, attributes);

        // When
        bookmarksMarkersStore.put(marker);

        // Then
        BookmarkMarker retrieved = bookmarksMarkersStore.get(bookmarkId);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getBookmarkId()).isEqualTo(bookmarkId);
        assertThat(retrieved.getResource()).isEqualTo(file);
        assertThat(retrieved.getLineNumber()).isEqualTo(10);
    }

    public void testPutAndGetByVirtualFile() {
        // Given
        VirtualFile file = myFixture.getTempDirFixture().createFile("test.txt");
        BookmarkId bookmarkId1 = new BookmarkId("bookmark1");
        BookmarkId bookmarkId2 = new BookmarkId("bookmark2");
        BookmarkMarker marker1 = new BookmarkMarker(file, bookmarkId1, Map.of(BookmarkMarker.LINE_NUMBER, "10"));
        BookmarkMarker marker2 = new BookmarkMarker(file, bookmarkId2, Map.of(BookmarkMarker.LINE_NUMBER, "20"));

        // When
        bookmarksMarkersStore.put(marker1);
        bookmarksMarkersStore.put(marker2);

        // Then
        List<BookmarkMarker> markers = bookmarksMarkersStore.get(file);
        assertThat(markers).hasSize(2);
        assertThat(markers).containsExactlyInAnyOrder(marker1, marker2);
    }

    public void testPutMultipleMarkersOnDifferentFiles() {
        // Given
        VirtualFile file1 = myFixture.getTempDirFixture().createFile("test1.txt");
        VirtualFile file2 = myFixture.getTempDirFixture().createFile("test2.txt");
        BookmarkId bookmarkId1 = new BookmarkId("bookmark1");
        BookmarkId bookmarkId2 = new BookmarkId("bookmark2");
        BookmarkMarker marker1 = new BookmarkMarker(file1, bookmarkId1, Map.of());
        BookmarkMarker marker2 = new BookmarkMarker(file2, bookmarkId2, Map.of());

        // When
        bookmarksMarkersStore.put(marker1);
        bookmarksMarkersStore.put(marker2);

        // Then
        assertThat(bookmarksMarkersStore.get(file1)).containsExactly(marker1);
        assertThat(bookmarksMarkersStore.get(file2)).containsExactly(marker2);
    }

    public void testRemoveMarker() {
        // Given
        VirtualFile file = myFixture.getTempDirFixture().createFile("test.txt");
        BookmarkId bookmarkId = new BookmarkId("bookmark1");
        BookmarkMarker marker = new BookmarkMarker(file, bookmarkId, Map.of());
        bookmarksMarkersStore.put(marker);

        // When
        BookmarkMarker removed = bookmarksMarkersStore.remove(bookmarkId);

        // Then
        assertThat(removed).isEqualTo(marker);
        assertThat(bookmarksMarkersStore.get(bookmarkId)).isNull();
        assertThat(bookmarksMarkersStore.get(file)).isEmpty();
    }

    public void testRemoveOneMarkerFromFileWithMultipleMarkers() {
        // Given
        VirtualFile file = myFixture.getTempDirFixture().createFile("test.txt");
        BookmarkId bookmarkId1 = new BookmarkId("bookmark1");
        BookmarkId bookmarkId2 = new BookmarkId("bookmark2");
        BookmarkMarker marker1 = new BookmarkMarker(file, bookmarkId1, Map.of());
        BookmarkMarker marker2 = new BookmarkMarker(file, bookmarkId2, Map.of());
        bookmarksMarkersStore.put(marker1);
        bookmarksMarkersStore.put(marker2);

        // When
        bookmarksMarkersStore.remove(bookmarkId1);

        // Then
        assertThat(bookmarksMarkersStore.get(bookmarkId1)).isNull();
        assertThat(bookmarksMarkersStore.get(file)).containsExactly(marker2);
    }

    public void testGetNonExistentBookmarkReturnsNull() {
        // When
        BookmarkMarker marker = bookmarksMarkersStore.get(new BookmarkId("nonexistent"));

        // Then
        assertThat(marker).isNull();
    }

    public void testGetNonExistentFileReturnsEmptyList() {
        // Given
        VirtualFile file = myFixture.getTempDirFixture().createFile("test.txt");

        // When
        List<BookmarkMarker> markers = bookmarksMarkersStore.get(file);

        // Then
        assertThat(markers).isEmpty();
    }

    public void testPutReplacesExistingMarker() {
        // Given
        VirtualFile file = myFixture.getTempDirFixture().createFile("test.txt");
        BookmarkId bookmarkId = new BookmarkId("bookmark1");
        BookmarkMarker marker1 = new BookmarkMarker(file, bookmarkId, Map.of(BookmarkMarker.LINE_NUMBER, "10"));
        BookmarkMarker marker2 = new BookmarkMarker(file, bookmarkId, Map.of(BookmarkMarker.LINE_NUMBER, "20"));
        bookmarksMarkersStore.put(marker1);

        // When
        BookmarkMarker previous = bookmarksMarkersStore.put(marker2);

        // Then
        assertThat(previous).isEqualTo(marker1);
        assertThat(bookmarksMarkersStore.get(bookmarkId)).isEqualTo(marker2);
        assertThat(bookmarksMarkersStore.get(file)).containsExactly(marker2); // Old marker is removed, only new one remains
    }

    public void testGetStateAndLoadState() {
        // Given
        VirtualFile file1 = myFixture.getTempDirFixture().createFile("test1.txt");
        VirtualFile file2 = myFixture.getTempDirFixture().createFile("test2.txt");
        BookmarkId bookmarkId1 = new BookmarkId("bookmark1");
        BookmarkId bookmarkId2 = new BookmarkId("bookmark2");
        Map<String, String> attributes1 = Map.of(
                BookmarkMarker.LINE_NUMBER, "10",
                "customAttr", "value1"
        );
        Map<String, String> attributes2 = Map.of(
                BookmarkMarker.LINE_NUMBER, "20"
        );
        BookmarkMarker marker1 = new BookmarkMarker(file1, bookmarkId1, attributes1);
        BookmarkMarker marker2 = new BookmarkMarker(file2, bookmarkId2, attributes2);
        bookmarksMarkersStore.put(marker1);
        bookmarksMarkersStore.put(marker2);

        // When
        Element state = bookmarksMarkersStore.getState();
        BookmarksMarkersStore newMap = new BookmarksMarkersStore();
        newMap.loadState(state);

        // Then
        BookmarkMarker retrievedMarker1 = newMap.get(bookmarkId1);
        assertThat(retrievedMarker1).isNotNull();
        assertThat(retrievedMarker1.getBookmarkId()).isEqualTo(bookmarkId1);
        assertThat(retrievedMarker1.getResource().getUrl()).isEqualTo(file1.getUrl());
        assertThat(retrievedMarker1.getLineNumber()).isEqualTo(10);
        assertThat(retrievedMarker1.getAttributes()).containsEntry("customAttr", "value1");

        BookmarkMarker retrievedMarker2 = newMap.get(bookmarkId2);
        assertThat(retrievedMarker2).isNotNull();
        assertThat(retrievedMarker2.getBookmarkId()).isEqualTo(bookmarkId2);
        assertThat(retrievedMarker2.getResource().getUrl()).isEqualTo(file2.getUrl());
        assertThat(retrievedMarker2.getLineNumber()).isEqualTo(20);
    }

    public void testGetStateWithEmptyMap() {
        // When
        Element state = bookmarksMarkersStore.getState();

        // Then
        assertThat(state.getName()).isEqualTo("BookmarkMarkers");
        assertThat(state.getChildren()).isEmpty();
    }
}
