
package mesfavoris.internal.persistence;

import com.google.common.collect.Lists;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.BookmarksException;
import mesfavoris.internal.remote.InMemoryRemoteBookmarksStore;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.modification.BookmarksTreeModifier;
import mesfavoris.remote.ConflictException;
import mesfavoris.remote.RemoteBookmarkFolder;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.remote.RemoteBookmarksTree;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeGenerator;
import mesfavoris.tests.commons.bookmarks.IncrementalIDGenerator;
import mesfavoris.tests.commons.bookmarks.RandomModificationApplier;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

public class RemoteBookmarksSaverTest extends BasePlatformTestCase {
    private static final int MAX_SAVE_ATTEMPTS = 3;
    private RemoteBookmarksStoreManager remoteBookmarksStoreManager;
    private ConflictingRemoteBookmarksStore remoteBookmarksStore;
    private RemoteBookmarksSaver saver;
    private BookmarksTree originalBookmarksTree;
    private final IncrementalIDGenerator incrementalIDGenerator = new IncrementalIDGenerator();
    private ProgressIndicator progressIndicator;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.remoteBookmarksStore = new ConflictingRemoteBookmarksStore(getProject());
        remoteBookmarksStoreManager = new RemoteBookmarksStoreManager(() -> Lists.newArrayList(remoteBookmarksStore));
        saver = new RemoteBookmarksSaver(remoteBookmarksStoreManager, MAX_SAVE_ATTEMPTS, Duration.ofMillis(1));
        originalBookmarksTree = new BookmarksTreeGenerator(incrementalIDGenerator, 5, 3, 2).build();
        progressIndicator = mock(ProgressIndicator.class);
        addAllTopLevelBookmarkFoldersToRemoteBookmarksStore();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private void addAllTopLevelBookmarkFoldersToRemoteBookmarksStore() throws IOException {
        for (Bookmark bookmark : originalBookmarksTree.getChildren(originalBookmarksTree.getRootFolder().getId())) {
            if (bookmark instanceof BookmarkFolder) {
                remoteBookmarksStore.add(originalBookmarksTree, bookmark.getId(), progressIndicator);
            }
        }
    }

    public void testMoveRemoteBookmarkFolder() throws BookmarksException, IOException {
        // Given
        BookmarksTreeModifier bookmarksTreeModifier = new BookmarksTreeModifier(originalBookmarksTree);
        BookmarkId rootId = bookmarksTreeModifier.getCurrentTree().getRootFolder().getId();
        bookmarksTreeModifier.moveBefore(
                Lists.newArrayList(bookmarksTreeModifier.getCurrentTree().getChildren(rootId).get(2).getId()), rootId,
                bookmarksTreeModifier.getCurrentTree().getChildren(rootId).get(0).getId());

        // When
        boolean saved = saver.applyModificationsToRemoteBookmarksStores(bookmarksTreeModifier.getModifications(),
                progressIndicator);

        // Then
        assertFalse(saved);
        verify(bookmarksTreeModifier.getCurrentTree());
    }

    public void testApplyRandomModifications() throws Exception {
        // Given
        BookmarksTreeModifier bookmarksTreeModifier = new BookmarksTreeModifier(originalBookmarksTree);
        randomModifications(bookmarksTreeModifier, 30);

        // When
        saver.applyModificationsToRemoteBookmarksStores(bookmarksTreeModifier.getModifications(),
                progressIndicator);

        // Then
        verify(bookmarksTreeModifier.getCurrentTree());
    }

    public void testRetriesSaveOnConflict() throws Exception {
        // Given
        BookmarksTreeModifier bookmarksTreeModifier = renameBookmarkInRemoteFolder();
        remoteBookmarksStore.conflictsBeforeSave = MAX_SAVE_ATTEMPTS - 1;

        // When
        saver.applyModificationsToRemoteBookmarksStores(bookmarksTreeModifier.getModifications(),
                progressIndicator);

        // Then
        assertEquals(MAX_SAVE_ATTEMPTS, remoteBookmarksStore.saveAttempts);
        verify(bookmarksTreeModifier.getCurrentTree());
    }

    public void testGivesUpWhenRemoteKeepsConflicting() throws Exception {
        // Given
        BookmarksTreeModifier bookmarksTreeModifier = renameBookmarkInRemoteFolder();
        remoteBookmarksStore.conflictsBeforeSave = Integer.MAX_VALUE;

        // When
        Throwable thrown = catchThrowable(() -> saver.applyModificationsToRemoteBookmarksStores(
                bookmarksTreeModifier.getModifications(), progressIndicator));

        // Then
        assertThat(thrown).isInstanceOf(BookmarksException.class)
                .cause().hasMessageContaining("still conflicting after " + MAX_SAVE_ATTEMPTS + " attempts");
        assertEquals(MAX_SAVE_ATTEMPTS, remoteBookmarksStore.saveAttempts);
    }

    private BookmarksTreeModifier renameBookmarkInRemoteFolder() {
        BookmarksTreeModifier bookmarksTreeModifier = new BookmarksTreeModifier(originalBookmarksTree);
        BookmarkId remoteFolderId = remoteBookmarksStore.getRemoteBookmarkFolders().iterator().next()
                .getBookmarkFolderId();
        BookmarkId bookmarkId = originalBookmarksTree.getChildren(remoteFolderId).get(0).getId();
        bookmarksTreeModifier.setPropertyValue(bookmarkId, Bookmark.PROPERTY_NAME, "renamed");
        return bookmarksTreeModifier;
    }

    private void verify(BookmarksTree bookmarksTree) throws IOException {
        for (RemoteBookmarkFolder remoteBookmarkFolder : remoteBookmarksStore.getRemoteBookmarkFolders()) {
            assertEquals(bookmarksTree.subTree(remoteBookmarkFolder.getBookmarkFolderId()).toString(),
                    remoteBookmarksStore.load(remoteBookmarkFolder.getBookmarkFolderId(), progressIndicator)
                            .getBookmarksTree().toString());
        }
    }

    private void randomModifications(BookmarksTreeModifier bookmarksTreeModifier, int n) {
        for (int i = 0; i < n; i++) {
            randomModification(bookmarksTreeModifier);
        }
    }

    private void randomModification(BookmarksTreeModifier bookmarksTreeModifier) {
        Predicate<Bookmark> onlyUnderRemoteBookmarkFolder = bookmark -> remoteBookmarksStoreManager
                .getRemoteBookmarkFolder(bookmark.getId()).isEmpty()
                && remoteBookmarksStoreManager.getRemoteBookmarkFolderContaining(bookmarksTreeModifier.getCurrentTree(),
                bookmark.getId()).isPresent();
        RandomModificationApplier randomModificationApplier = new RandomModificationApplier(incrementalIDGenerator,
                onlyUnderRemoteBookmarkFolder);
        randomModificationApplier.applyRandomModification(bookmarksTreeModifier, new PrintWriter(new StringWriter()));

    }


    private static class ConflictingRemoteBookmarksStore extends InMemoryRemoteBookmarksStore {
        private int conflictsBeforeSave;
        private int saveAttempts;

        ConflictingRemoteBookmarksStore(Project project) {
            super(project);
        }

        @Override
        public RemoteBookmarksTree save(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId, String etag,
                                        ProgressIndicator indicator) throws ConflictException {
            saveAttempts++;
            if (saveAttempts <= conflictsBeforeSave) {
                throw new ConflictException();
            }
            return super.save(bookmarksTree, bookmarkFolderId, etag, indicator);
        }
    }

}
