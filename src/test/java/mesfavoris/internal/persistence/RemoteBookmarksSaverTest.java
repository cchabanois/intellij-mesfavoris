
package mesfavoris.internal.persistence;

import com.google.common.collect.Lists;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import mesfavoris.BookmarksException;
import mesfavoris.internal.remote.InMemoryRemoteBookmarksStore;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.modification.BookmarksTreeModifier;
import mesfavoris.remote.RemoteBookmarkFolder;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.tests.commons.bookmarks.BookmarksTreeGenerator;
import mesfavoris.tests.commons.bookmarks.IncrementalIDGenerator;
import mesfavoris.tests.commons.bookmarks.RandomModificationApplier;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Predicate;

import static org.mockito.Mockito.mock;

public class RemoteBookmarksSaverTest extends BasePlatformTestCase {
    private RemoteBookmarksStoreManager remoteBookmarksStoreManager;
    private InMemoryRemoteBookmarksStore remoteBookmarksStore;
    private RemoteBookmarksSaver saver;
    private BookmarksTree originalBookmarksTree;
    private final IncrementalIDGenerator incrementalIDGenerator = new IncrementalIDGenerator();
    private ProgressIndicator progressIndicator;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.remoteBookmarksStore = new InMemoryRemoteBookmarksStore(getProject());
        remoteBookmarksStoreManager = new RemoteBookmarksStoreManager(() -> Lists.newArrayList(remoteBookmarksStore));
        saver = new RemoteBookmarksSaver(remoteBookmarksStoreManager);
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

}
