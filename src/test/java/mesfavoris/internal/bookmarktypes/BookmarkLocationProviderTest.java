package mesfavoris.internal.bookmarktypes;

import com.intellij.mock.MockProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IBookmarkLocationProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class BookmarkLocationProviderTest {

    @Test
    public void testReturnBestLocation() {
        // Given
        Bookmark bookmark = new Bookmark(new BookmarkId());
        IBookmarkLocationProvider bookmarkLocationProvider1 = mock(IBookmarkLocationProvider.class);
        IBookmarkLocationProvider bookmarkLocationProvider2 = mock(IBookmarkLocationProvider.class);
        IBookmarkLocationProvider bookmarkLocationProvider3 = mock(IBookmarkLocationProvider.class);
        IBookmarkLocation bookmarkLocation1 = mock(IBookmarkLocation.class);
        IBookmarkLocation bookmarkLocation2 = mock(IBookmarkLocation.class);
        IBookmarkLocation bookmarkLocation3 = null;
        when(bookmarkLocation1.getScore()).thenReturn(0.5f);
        when(bookmarkLocation2.getScore()).thenReturn(0.75f);
        when(bookmarkLocationProvider1.getBookmarkLocation(any(Project.class), eq(bookmark), any(ProgressIndicator.class)))
                .thenReturn(bookmarkLocation1);
        when(bookmarkLocationProvider2.getBookmarkLocation(any(Project.class), eq(bookmark), any(ProgressIndicator.class)))
                .thenReturn(bookmarkLocation2);
        when(bookmarkLocationProvider3.getBookmarkLocation(any(Project.class), eq(bookmark), any(ProgressIndicator.class)))
                .thenReturn(bookmarkLocation3);
        BookmarkLocationProvider bookmarkLocationProvider = new BookmarkLocationProvider(
                Arrays.asList(bookmarkLocationProvider1, bookmarkLocationProvider2, bookmarkLocationProvider3));

        // When
        IBookmarkLocation bookmarkLocation = bookmarkLocationProvider.getBookmarkLocation(mock(Project.class), bookmark,
                mock(ProgressIndicator.class));

        // Then
        assertThat(bookmarkLocation).isEqualTo(bookmarkLocation2);
    }

    @Test
    public void testReturnFirstLocationWithMaxScore() {
        // Given
        Bookmark bookmark = new Bookmark(new BookmarkId());
        IBookmarkLocationProvider bookmarkLocationProvider1 = mock(IBookmarkLocationProvider.class);
        IBookmarkLocationProvider bookmarkLocationProvider2 = mock(IBookmarkLocationProvider.class);
        IBookmarkLocation bookmarkLocation1 = mock(IBookmarkLocation.class);
        IBookmarkLocation bookmarkLocation2 = mock(IBookmarkLocation.class);
        when(bookmarkLocation1.getScore()).thenReturn(1.0f);
        when(bookmarkLocation2.getScore()).thenReturn(1.0f);
        when(bookmarkLocationProvider1.getBookmarkLocation(any(Project.class), eq(bookmark), any(ProgressIndicator.class)))
                .thenReturn(bookmarkLocation1);
        when(bookmarkLocationProvider2.getBookmarkLocation(any(Project.class), eq(bookmark), any(ProgressIndicator.class)))
                .thenReturn(bookmarkLocation2);
        BookmarkLocationProvider bookmarkLocationProvider = new BookmarkLocationProvider(
                Arrays.asList(bookmarkLocationProvider1, bookmarkLocationProvider2));

        // When
        IBookmarkLocation bookmarkLocation = bookmarkLocationProvider.getBookmarkLocation(mock(Project.class), bookmark,
                mock(ProgressIndicator.class));

        // Then
        assertThat(bookmarkLocation).isEqualTo(bookmarkLocation1);
        verify(bookmarkLocationProvider2, never()).getBookmarkLocation(any(Project.class), eq(bookmark), any(ProgressIndicator.class));
    }

}
