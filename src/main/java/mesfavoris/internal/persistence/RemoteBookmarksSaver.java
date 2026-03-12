package mesfavoris.internal.persistence;

import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.BookmarksException;
import mesfavoris.internal.model.copy.BookmarksCopier;
import mesfavoris.internal.model.replay.ModificationsReplayer;
import mesfavoris.internal.model.utils.BookmarksTreeUtils;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.modification.*;
import mesfavoris.remote.*;

import java.io.IOException;
import java.util.*;

/**
 * Apply changes to remote bookmarks
 *
 * @author cchabanois
 */
public class RemoteBookmarksSaver {
    private static final Logger LOG = Logger.getInstance(RemoteBookmarksSaver.class);
    private final RemoteBookmarksStoreManager remoteBookmarksStoreManager;

    public RemoteBookmarksSaver(RemoteBookmarksStoreManager remoteBookmarksStoreManager) {
        this.remoteBookmarksStoreManager = remoteBookmarksStoreManager;
    }

    /**
     * Apply given modifications to remote bookmark stores
     *
     * @param bookmarksModifications
     * @param progressIndicator
     * @return true if remote bookmark stores modified
     * @throws BookmarksException
     */
    public boolean applyModificationsToRemoteBookmarksStores(List<BookmarksModification> bookmarksModifications,
                                                             ProgressIndicator progressIndicator) throws BookmarksException {
        Map<RemoteBookmarkFolder, List<BookmarksModification>> remoteBookmarkFolders = getRemoteBookmarkFolders(
                bookmarksModifications);
        if (remoteBookmarkFolders.isEmpty()) {
            return false;
        }
        progressIndicator.setText("Saving to remote stores");
        try {
            int workDone = 0;
            for (Map.Entry<RemoteBookmarkFolder, List<BookmarksModification>> entry : remoteBookmarkFolders
                    .entrySet()) {
                progressIndicator.setFraction((double) workDone / remoteBookmarkFolders.size());
                applyModificationsToRemoteBookmarkFolder(entry.getKey(), entry.getValue(), progressIndicator);
                workDone++;
            }
            return true;
        } catch (IOException e) {
            LOG.error("Could not save bookmarks", e);
            throw new BookmarksException("Could not save bookmarks", e);
        }

    }

    /**
     * @param remoteBookmarkFolder
     * @param modifications
     * @param progressIndicator
     * @return true if
     * @throws IOException
     */
    private void applyModificationsToRemoteBookmarkFolder(RemoteBookmarkFolder remoteBookmarkFolder,
                                                          List<BookmarksModification> modifications, ProgressIndicator progressIndicator) throws IOException {
        IRemoteBookmarksStore store = remoteBookmarksStoreManager
                .getRemoteBookmarksStore(remoteBookmarkFolder.getRemoteBookmarkStoreId()).get();

        progressIndicator.setText("Saving to remote bookmark folder");
        while (true) {
            progressIndicator.setFraction(progressIndicator.getFraction() + 0.5 * (1.0 - progressIndicator.getFraction()));
            RemoteBookmarksTree remoteBookmarksTree = store.load(remoteBookmarkFolder.getBookmarkFolderId(),
                    progressIndicator);
            ModificationsReplayer modificationsReplayer = new ModificationsReplayer(modifications);
            BookmarksTreeModifier remoteBookmarksTreeModifier = new BookmarksTreeModifier(
                    remoteBookmarksTree.getBookmarksTree());
            List<BookmarksModification> modificationsNotReplayed = modificationsReplayer
                    .replayModifications(remoteBookmarksTreeModifier);
            if (remoteBookmarksTreeModifier.getOriginalTree() == remoteBookmarksTreeModifier.getCurrentTree()) {
                return;
            }
            try {
                progressIndicator.setFraction(progressIndicator.getFraction() + 0.5 * (1.0 - progressIndicator.getFraction()));
                store.save(remoteBookmarksTreeModifier.getCurrentTree(),
                        remoteBookmarksTreeModifier.getCurrentTree().getRootFolder().getId(),
                        remoteBookmarksTree.getEtag(), progressIndicator);
                return;
            } catch (ConflictException e) {
                // conflict occurred, reload and retry
            }
        }
    }

    private Map<RemoteBookmarkFolder, List<BookmarksModification>> getRemoteBookmarkFolders(
            List<BookmarksModification> modifications) {
		Map<RemoteBookmarkFolder, List<BookmarksModification>> result = new HashMap<>();
		for (BookmarksModification event : modifications) {
			if (event instanceof BookmarkDeletedModification) {
				BookmarkDeletedModification modification = (BookmarkDeletedModification) event;
				Optional<RemoteBookmarkFolder> remoteBookmarkFolder = remoteBookmarksStoreManager
						.getRemoteBookmarkFolderContaining(modification.getSourceTree(), modification.getBookmarkId());
				if (remoteBookmarkFolder.isPresent()) {
					add(result, remoteBookmarkFolder.get(), modification);
				}
			} else if (event instanceof BookmarksAddedModification) {
				BookmarksAddedModification modification = (BookmarksAddedModification) event;
				Optional<RemoteBookmarkFolder> remoteBookmarkFolder = remoteBookmarksStoreManager
						.getRemoteBookmarkFolderContaining(modification.getSourceTree(), modification.getParentId());
				if (remoteBookmarkFolder.isPresent()) {
					add(result, remoteBookmarkFolder.get(), modification);
				}
			} else if (event instanceof BookmarkPropertiesModification) {
				BookmarkPropertiesModification modification = (BookmarkPropertiesModification) event;
				Optional<RemoteBookmarkFolder> remoteBookmarkFolder = remoteBookmarksStoreManager
						.getRemoteBookmarkFolderContaining(modification.getSourceTree(), modification.getBookmarkId());
				if (remoteBookmarkFolder.isPresent()) {
					add(result, remoteBookmarkFolder.get(), modification);
				}
			} else if (event instanceof BookmarksMovedModification) {
				BookmarksMovedModification modification = (BookmarksMovedModification) event;
				Optional<RemoteBookmarkFolder> remoteBookmarkFolderSource = getSourceRemoteBookmarkFolder(modification);
				Optional<RemoteBookmarkFolder> remoteBookmarkFolderTarget = remoteBookmarksStoreManager
						.getRemoteBookmarkFolderContaining(modification.getSourceTree(), modification.getNewParentId());
				if (remoteBookmarkFolderSource.isPresent() && remoteBookmarkFolderTarget.isPresent()
						&& remoteBookmarkFolderSource.get().equals(remoteBookmarkFolderTarget.get())) {
					add(result, remoteBookmarkFolderSource.get(), modification);
				} else if (remoteBookmarkFolderSource.isPresent() || remoteBookmarkFolderTarget.isPresent()) {
					getRemoteBookmarkFolders(movedModificationToDeleteAddModifications(modification))
							.forEach((remoteBookmarkFolder, deleteAddModifications) -> add(result, remoteBookmarkFolder,
									deleteAddModifications));
				}
			}
		}
		return result;
	}

	private Optional<RemoteBookmarkFolder> getSourceRemoteBookmarkFolder(BookmarksMovedModification modification) {
		if (remoteBookmarksStoreManager.getRemoteBookmarkFolder(modification.getBookmarkIds().get(0)).isPresent()) {
			// the remote bookmark folder has been moved
			return Optional.empty();
		} else {
			return remoteBookmarksStoreManager.getRemoteBookmarkFolderContaining(modification.getSourceTree(),
					modification.getBookmarkIds().get(0));
		}
	}

	private List<BookmarksModification> movedModificationToDeleteAddModifications(
			BookmarksMovedModification modification) {
		BookmarksTreeModifier bookmarksTreeModifier = new BookmarksTreeModifier(modification.getSourceTree());
		for (BookmarkId bookmarkId : modification.getBookmarkIds()) {
			bookmarksTreeModifier.deleteBookmark(bookmarkId, true);
		}
		BookmarksCopier bookmarksCopier = new BookmarksCopier(modification.getSourceTree(), bookmarkId -> bookmarkId);
		Bookmark bookmarkBefore = BookmarksTreeUtils.getBookmarkBefore(modification.getTargetTree(),
				modification.getBookmarkIds().get(0),
				b -> bookmarksTreeModifier.getCurrentTree().getBookmark(b.getId()) != null);
		bookmarksCopier.copyAfter(bookmarksTreeModifier, modification.getNewParentId(),
				bookmarkBefore == null ? null : bookmarkBefore.getId(), modification.getBookmarkIds());
		return bookmarksTreeModifier.getModifications();
	}

	private void add(Map<RemoteBookmarkFolder, List<BookmarksModification>> map,
			RemoteBookmarkFolder remoteBookmarkFolder, BookmarksModification modification) {
		add(map, remoteBookmarkFolder, Lists.newArrayList(modification));
	}

	private void add(Map<RemoteBookmarkFolder, List<BookmarksModification>> map,
			RemoteBookmarkFolder remoteBookmarkFolder, List<BookmarksModification> modifications) {
		List<BookmarksModification> list = map.get(remoteBookmarkFolder);
		if (list == null) {
			list = new ArrayList<>();
			map.put(remoteBookmarkFolder, list);
		}
		list.addAll(modifications);
	}

}
