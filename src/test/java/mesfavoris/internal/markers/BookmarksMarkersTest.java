package mesfavoris.internal.markers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.UIUtil;
import mesfavoris.BookmarksException;
import mesfavoris.IBookmarksMarkers;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.service.BookmarksService;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static mesfavoris.tests.commons.waits.Waiter.waitUntil;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_LINE_NUMBER;
import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROP_WORKSPACE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class BookmarksMarkersTest extends BasePlatformTestCase {
    private IBookmarksMarkers bookmarksMarkers;
    private BookmarkDatabase bookmarkDatabase;
    private BookmarkId rootFolderId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        BookmarksService bookmarksService = getProject().getService(BookmarksService.class);
        this.bookmarkDatabase = bookmarksService.getBookmarkDatabase();
        this.bookmarksMarkers = bookmarksService.getBookmarksMarkers();
        this.rootFolderId = bookmarkDatabase.getBookmarksTree().getRootFolder().getId();
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

	public void testMarkerAddedWhenBookmarkAdded() throws Exception {
		// Given
		myFixture.copyDirectoryToProject("bookmarkMarkersTest", "testMarkerAddedWhenBookmarkAdded");
		Bookmark bookmark = new Bookmark(new BookmarkId(), ImmutableMap.of(
				PROP_WORKSPACE_PATH, "testMarkerAddedWhenBookmarkAdded/file.txt",
				PROP_LINE_NUMBER, "0"
		));

		// When
		addBookmark(rootFolderId, bookmark);
		BookmarkMarker marker = waitUntil("Cannot find marker", () -> {
			UIUtil.dispatchAllInvocationEvents();  // Process pending invokeLater events
			return findBookmarkMarker(bookmark.getId());
		});

		// Then
		assertThat(marker).isNotNull();
		assertThat(Integer.parseInt(marker.getAttributes().get(BookmarkMarker.LINE_NUMBER))).isEqualTo(0);
	}

	public void testMarkerDeletedWhenBookmarkDeleted() throws Exception {
		// Given
		myFixture.copyDirectoryToProject("bookmarkMarkersTest", "testMarkerDeletedWhenBookmarkDeleted");
		Bookmark bookmark = new Bookmark(new BookmarkId(), ImmutableMap.of(
				PROP_WORKSPACE_PATH, "testMarkerDeletedWhenBookmarkDeleted/file.txt",
				PROP_LINE_NUMBER, "0"
		));
		addBookmark(rootFolderId, bookmark);
		waitUntil("Cannot find marker", () -> {
			UIUtil.dispatchAllInvocationEvents();
			return findBookmarkMarker(bookmark.getId());
		});

		// When
		deleteBookmark(bookmark.getId());

		// Then
		waitUntil("Marker not deleted", () -> {
			UIUtil.dispatchAllInvocationEvents();
			return findBookmarkMarker(bookmark.getId()) == null;
		});
	}

	public void testMarkerDeletedWhenBookmarkParentDeletedRecursively() throws Exception {
		// Given
		myFixture.copyDirectoryToProject("bookmarkMarkersTest", "testMarkerDeletedWhenBookmarkParentDeletedRecursively");
		BookmarkFolder bookmarkFolder = new BookmarkFolder(new BookmarkId(), "folder");
		addBookmark(rootFolderId, bookmarkFolder);
		Bookmark bookmark = new Bookmark(new BookmarkId(), ImmutableMap.of(
				PROP_WORKSPACE_PATH, "testMarkerDeletedWhenBookmarkDeleted/file.txt",
				PROP_LINE_NUMBER, "0"
		));
		addBookmark(bookmarkFolder.getId(), bookmark);
		waitUntil("Cannot find marker", () -> {
			UIUtil.dispatchAllInvocationEvents();
			return findBookmarkMarker(bookmark.getId());
		});

		// When
		deleteBookmarkRecursively(bookmarkFolder.getId());

		// Then
		waitUntil("Marker not deleted", () -> {
			UIUtil.dispatchAllInvocationEvents();
			return findBookmarkMarker(bookmark.getId()) == null;
		});
	}

	public void testMarkerModifiedWhenBookmarkChanged() throws Exception {
		// Given
		myFixture.copyDirectoryToProject("bookmarkMarkersTest", "testMarkerModifiedWhenBookmarkChanged");
		Bookmark bookmark = new Bookmark(new BookmarkId(), ImmutableMap.of(
				PROP_WORKSPACE_PATH, "testMarkerModifiedWhenBookmarkChanged/file.txt",
				PROP_LINE_NUMBER, "0"
		));
		addBookmark(rootFolderId, bookmark);
		waitUntil("Cannot find marker", () -> {
			UIUtil.dispatchAllInvocationEvents();
			return findBookmarkMarker(bookmark.getId());
		});

		// When
		modifyBookmark(bookmark.getId(), PROP_LINE_NUMBER, "1");

		// Then
		waitUntil("Marker not modified", () -> {
			UIUtil.dispatchAllInvocationEvents();
			BookmarkMarker marker = findBookmarkMarker(bookmark.getId());
			return marker != null && Integer.parseInt(marker.getAttributes().get(BookmarkMarker.LINE_NUMBER)) == 1;
		});
	}

	public void testMarkerReplacedWhenBookmarkResourceChanged() throws Exception {
		// Given
		myFixture.copyDirectoryToProject("bookmarkMarkersTest", "testMarkerReplacedWhenBookmarkResourceChanged");
		Bookmark bookmark = bookmarkWithPathAndLine("testMarkerReplacedWhenBookmarkResourceChanged/file.txt", 0);
        addBookmark(rootFolderId, bookmark);
        waitUntilBookmarkMarkerAtLine(bookmark.getId(), 0);

		// When
		modifyBookmark(bookmark.getId(), PROP_WORKSPACE_PATH, "testMarkerReplacedWhenBookmarkResourceChanged/file2.txt");

		// Then
		waitUntil("Marker not modified", () -> {
			UIUtil.dispatchAllInvocationEvents();
			BookmarkMarker m = findBookmarkMarker(bookmark.getId());
			return m != null && m.getResource().getPath().endsWith("/testMarkerReplacedWhenBookmarkResourceChanged/file2.txt");
		});
	}

    // TODO: Implement VirtualFileListener to handle automatic marker deletion when files are deleted
    // @Ignore("Requires VirtualFileListener implementation to handle automatic marker deletion when files are deleted")
    // public void testMarkerDeletedWhenFileDeleted() throws Exception {
    //     // Given
    //     myFixture.copyDirectoryToProject("bookmarkMarkersTest", "testMarkerDeletedWhenFileDeleted");
    //     Bookmark bookmark = new Bookmark(new BookmarkId(), ImmutableMap.of(
    //             PROP_WORKSPACE_PATH, "testMarkerDeletedWhenFileDeleted/file.txt",
    //             PROP_LINE_NUMBER, "0"
    //     ));
    //     addBookmark(rootFolderId, bookmark);
    //     waitUntil("Cannot find marker", () -> findBookmarkMarker(bookmark.getId()));
    //
    //     // When
    //     VirtualFile fileToDelete = myFixture.findFileInTempDir("testMarkerDeletedWhenFileDeleted/file.txt");
    //     WriteAction.runAndWait(() -> fileToDelete.delete(this));
    //
    //     // Then
    //     waitUntil("Marker not deleted", () -> findBookmarkMarker(bookmark.getId()) == null);
    // }

     public void testMarkerUpdatedWhenDocumentModified() throws Exception {
         // Given
         myFixture.copyDirectoryToProject("bookmarkMarkersTest", "testMarkerUpdatedWhenDocumentModified");
         Bookmark bookmark = bookmarkWithPathAndLine("testMarkerUpdatedWhenDocumentModified/file.txt", 2);  // Bookmark on "third line"
         addBookmark(rootFolderId, bookmark);
         waitUntilBookmarkMarkerAtLine(bookmark.getId(), 2);

         // When - Insert a new line at the beginning of the document
         myFixture.openFileInEditor(myFixture.findFileInTempDir("testMarkerUpdatedWhenDocumentModified/file.txt"));
         waitUntilBookmarkHighlighterAtLine(bookmark.getId(), 2);
         WriteCommandAction.runWriteCommandAction(getProject(), () -> myFixture.getEditor().getDocument().insertString(0, "New first line\n"));

         // Then - The marker should be updated to reflect the new line position (now line 3)
         waitUntilBookmarkMarkerAtLine(bookmark.getId(), 3);
     }

     private Bookmark bookmarkWithPathAndLine(String path, int lineNumber) {
         return new Bookmark(new BookmarkId(), ImmutableMap.of(
                 PROP_WORKSPACE_PATH, path,
                 PROP_LINE_NUMBER, Integer.toString(lineNumber)
         ));
     }

     private void waitUntilBookmarkHighlighterAtLine(BookmarkId bookmarkId, int lineNumber) throws TimeoutException {
         waitUntil("No bookmark highlighter found or highlighter not at expected line", () -> {
             UIUtil.dispatchAllInvocationEvents();
             List<RangeHighlighterEx> highlighters = BookmarksHighlighters.getBookmarksHighlighters(getProject(), myFixture.getEditor().getDocument());
             return highlighters.stream().anyMatch(highlighter -> {
                 BookmarkId highlighterBookmarkId = highlighter.getUserData(BookmarksHighlighters.BOOKMARK_ID_KEY);
                 return highlighterBookmarkId != null && highlighterBookmarkId.equals(bookmarkId) && highlighter.getDocument().getLineNumber(highlighter.getStartOffset()) == lineNumber;
             });
         });
     }

     private void waitUntilBookmarkMarkerAtLine(BookmarkId bookmarkId, int lineNumber) throws TimeoutException {
         waitUntil("No bookmark marker found or marker not at expected line", () -> {
             UIUtil.dispatchAllInvocationEvents();
             BookmarkMarker marker = findBookmarkMarker(bookmarkId);
             return marker != null && Integer.parseInt(marker.getAttributes().get(BookmarkMarker.LINE_NUMBER)) == lineNumber;
         });
     }

	private void addBookmark(BookmarkId parentId, Bookmark bookmark) throws BookmarksException {
		bookmarkDatabase.modify(bookmarksTreeModifier ->
				bookmarksTreeModifier.addBookmarks(parentId, Lists.newArrayList(bookmark))
		);
	}

	private void deleteBookmark(BookmarkId bookmarkId) throws BookmarksException {
		bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier.deleteBookmark(bookmarkId, false));
	}

	private void deleteBookmarkRecursively(BookmarkId bookmarkId) throws BookmarksException {
		bookmarkDatabase.modify(bookmarksTreeModifier -> bookmarksTreeModifier.deleteBookmark(bookmarkId, true));
	}

	private void modifyBookmark(BookmarkId bookmarkId, String propertyName, String propertyValue) throws BookmarksException {
		bookmarkDatabase.modify(bookmarksTreeModifier ->
				bookmarksTreeModifier.setPropertyValue(bookmarkId, propertyName, propertyValue)
		);
	}

	private BookmarkMarker findBookmarkMarker(BookmarkId bookmarkId) {
		return bookmarksMarkers.getMarker(bookmarkId);
	}
}
