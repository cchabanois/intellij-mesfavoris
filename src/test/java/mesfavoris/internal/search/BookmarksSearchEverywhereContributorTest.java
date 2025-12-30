package mesfavoris.internal.search;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.Processor;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.service.IBookmarksService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmark;
import static mesfavoris.tests.commons.bookmarks.BookmarkBuilder.bookmarkFolder;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BookmarksSearchEverywhereContributorTest extends BasePlatformTestCase {
    private BookmarksSearchEverywhereContributor contributor;
    private BookmarkDatabase bookmarkDatabase;
    private BookmarkId folder1Id;
    private BookmarkId bookmark1Id;
    private BookmarkId bookmark2Id;
    private BookmarkId bookmark3Id;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Create test data with unique IDs
        folder1Id = new BookmarkId();
        bookmark1Id = new BookmarkId();
        bookmark2Id = new BookmarkId();
        bookmark3Id = new BookmarkId();

        // Get service and database
        IBookmarksService bookmarksService = getProject().getService(IBookmarksService.class);
        bookmarkDatabase = bookmarksService.getBookmarkDatabase();
        BookmarkId rootFolderId = bookmarkDatabase.getBookmarksTree().getRootFolder().getId();

        // Add test bookmarks to the database
        bookmarkDatabase.modify(modifier -> {
            modifier.addBookmarks(rootFolderId, Arrays.asList(
                    bookmark(bookmark1Id, "Java Tutorial")
                            .withProperty(Bookmark.PROPERTY_COMMENT, "Learn Java basics").build(),
                    bookmark(bookmark2Id, "Python Guide").build(),
                    bookmarkFolder(folder1Id, "Development").build()));
            modifier.addBookmarks(folder1Id, Arrays.asList(
                    bookmark(bookmark3Id, "IntelliJ IDEA")
                            .withProperty(Bookmark.PROPERTY_COMMENT, "IDE for Java").build()));
        });

        // Create contributor with mock event
        AnActionEvent mockEvent = mock(AnActionEvent.class);
        when(mockEvent.getProject()).thenReturn(getProject());

        contributor = new BookmarksSearchEverywhereContributor(mockEvent);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetSearchProviderId() {
        // When
        String providerId = contributor.getSearchProviderId();

        // Then
        assertThat(providerId).isEqualTo("BookmarksSearchEverywhereContributor");
    }

    @Test
    public void testGetGroupName() {
        // When
        String groupName = contributor.getGroupName();

        // Then
        assertThat(groupName).isEqualTo("Mes Favoris");
    }

    @Test
    public void testFetchElementsWithEmptyPattern() {
        // Given
        List<BookmarkItem> results = new ArrayList<>();
        Processor<BookmarkItem> consumer = item -> {
            results.add(item);
            return true;
        };

        // When
        contributor.fetchElements("", new EmptyProgressIndicator(), consumer);

        // Then: Check that our test bookmarks are present (there may be others from the default database)
        assertThat(results).extracting(item -> item.getBookmark().getId())
                .contains(bookmark1Id, bookmark2Id, folder1Id, bookmark3Id);
    }

    @Test
    public void testFetchElementsWithMatchingPattern() {
        // Given
        List<BookmarkItem> results = new ArrayList<>();
        Processor<BookmarkItem> consumer = item -> {
            results.add(item);
            return true;
        };

        // When
        contributor.fetchElements("java", new EmptyProgressIndicator(), consumer);

        // Then
        assertThat(results).hasSize(2); // "Java Tutorial" and "IntelliJ IDEA" (comment contains "Java")
        assertThat(results).extracting(item -> item.getBookmark().getId())
                .containsExactlyInAnyOrder(bookmark1Id, bookmark3Id);
    }

    @Test
    public void testFetchElementsWithNonMatchingPattern() {
        // Given
        List<BookmarkItem> results = new ArrayList<>();
        Processor<BookmarkItem> consumer = item -> {
            results.add(item);
            return true;
        };

        // When
        contributor.fetchElements("nonexistent", new EmptyProgressIndicator(), consumer);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    public void testFetchElementsSearchInComments() {
        // Given
        List<BookmarkItem> results = new ArrayList<>();
        Processor<BookmarkItem> consumer = item -> {
            results.add(item);
            return true;
        };

        // When
        contributor.fetchElements("basics", new EmptyProgressIndicator(), consumer);

        // Then: Check that our bookmark with "basics" in comment is found
        assertThat(results).extracting(item -> item.getBookmark().getId())
                .contains(bookmark1Id);
    }
}

