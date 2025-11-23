package mesfavoris.internal.remote;

import com.google.common.collect.ImmutableMap;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.model.IBookmarksListener;
import mesfavoris.model.modification.BookmarkDeletedModification;
import mesfavoris.model.modification.BookmarksModification;
import mesfavoris.remote.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class InMemoryRemoteBookmarksStore extends AbstractRemoteBookmarksStore implements IBookmarksListener {
	private final AtomicReference<State> state = new AtomicReference<>(State.disconnected);
	private final ConcurrentMap<BookmarkId, InMemoryRemoteBookmarksTree> inMemoryRemoteBookmarksTrees = new ConcurrentHashMap<>();
	private final ConcurrentMap<BookmarkId, Map<String, String>> remoteBookmarkFolderProperties = new ConcurrentHashMap<>();

	public InMemoryRemoteBookmarksStore(Project project) {
		super(project);
		RemoteBookmarksStoreDescriptor descriptor = new RemoteBookmarksStoreDescriptor(
				"inMemory",
				"In Memory",
				AllIcons.General.Information,
				AllIcons.General.Information
		);
		init(descriptor);
	}

	@Override
	public UserInfo getUserInfo() {
		return null;
	}

	@Override
	public void connect(ProgressIndicator indicator) {
		state.set(State.connected);
		postConnected();
	}

	@Override
	public void disconnect(ProgressIndicator indicator) {
		state.set(State.disconnected);
		postDisconnected();
	}

	@Override
	public State getState() {
		return state.get();
	}

	@Override
	public RemoteBookmarksTree add(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId, ProgressIndicator indicator)
			throws IOException {
		String etag = UUID.randomUUID().toString();
		BookmarksTree subTree = bookmarksTree.subTree(bookmarkFolderId);
		inMemoryRemoteBookmarksTrees.put(bookmarkFolderId, new InMemoryRemoteBookmarksTree(subTree, etag));
		remoteBookmarkFolderProperties.put(bookmarkFolderId, new ConcurrentHashMap<>());
		return new RemoteBookmarksTree(this, subTree, etag);
	}

	public void addRemoteBookmarkFolderProperty(BookmarkId bookmarkId, String key, String value) {
		Map<String, String> map = remoteBookmarkFolderProperties.get(bookmarkId);
		if (map == null) {
			return;
		}
		map.put(key, value);
	}

	private Map<String, String> getRemoteBookmarkFolderProperties(BookmarkId bookmarkId) {
		return ImmutableMap.copyOf(remoteBookmarkFolderProperties.get(bookmarkId));
	}

	@Override
	public void remove(BookmarkId bookmarkFolderId, ProgressIndicator indicator) {
		inMemoryRemoteBookmarksTrees.remove(bookmarkFolderId);
		remoteBookmarkFolderProperties.remove(bookmarkFolderId);
	}

	@Override
	public Set<RemoteBookmarkFolder> getRemoteBookmarkFolders() {
		return inMemoryRemoteBookmarksTrees
				.keySet().stream().map(bookmarkFolderId -> new RemoteBookmarkFolder(getDescriptor().id(),
						bookmarkFolderId, getRemoteBookmarkFolderProperties(bookmarkFolderId)))
				.collect(Collectors.toSet());
	}

	@Override
	public Optional<RemoteBookmarkFolder> getRemoteBookmarkFolder(BookmarkId bookmarkFolderId) {
		if (inMemoryRemoteBookmarksTrees.get(bookmarkFolderId) != null) {
			return Optional.of(new RemoteBookmarkFolder(getDescriptor().id(), bookmarkFolderId,
					getRemoteBookmarkFolderProperties(bookmarkFolderId)));
		} else {
			return Optional.empty();
		}
	}

	@Override
	public RemoteBookmarksTree load(BookmarkId bookmarkFolderId, ProgressIndicator indicator) {
		InMemoryRemoteBookmarksTree inMemoryRemoteBookmarksTree = inMemoryRemoteBookmarksTrees.get(bookmarkFolderId);
		if (inMemoryRemoteBookmarksTree == null) {
			throw new IllegalArgumentException();
		}
		return new RemoteBookmarksTree(this, inMemoryRemoteBookmarksTree.bookmarksTree,
				inMemoryRemoteBookmarksTree.etag);
	}

	@Override
	public RemoteBookmarksTree save(BookmarksTree bookmarksTree, BookmarkId bookmarkFolderId, String etag,
			ProgressIndicator indicator) throws ConflictException {
		InMemoryRemoteBookmarksTree inMemoryRemoteBookmarksTree = inMemoryRemoteBookmarksTrees.get(bookmarkFolderId);
		if (inMemoryRemoteBookmarksTree == null) {
			throw new IllegalArgumentException();
		}
		if (!inMemoryRemoteBookmarksTree.etag.equals(etag)) {
			throw new ConflictException();
		}
		etag = UUID.randomUUID().toString();
		BookmarksTree subTree = bookmarksTree.subTree(bookmarkFolderId);
		InMemoryRemoteBookmarksTree newInMemoryRemoteBookmarksTree = new InMemoryRemoteBookmarksTree(subTree, etag);
		if (!inMemoryRemoteBookmarksTrees.replace(bookmarkFolderId, inMemoryRemoteBookmarksTree,
				newInMemoryRemoteBookmarksTree)) {
			throw new ConflictException();
		}
		return new RemoteBookmarksTree(this, subTree, etag);
	}

	@Override
	public void bookmarksModified(List<BookmarksModification> modifications) {
		for (BookmarkId bookmarkFolderId : getDeletedMappedBookmarkFolders(modifications)) {
			inMemoryRemoteBookmarksTrees.remove(bookmarkFolderId);
		}
	}

	private List<BookmarkId> getDeletedMappedBookmarkFolders(List<BookmarksModification> events) {
		return events.stream().filter(p -> p instanceof BookmarkDeletedModification)
				.map(p -> (BookmarkDeletedModification) p)
				.filter(p -> inMemoryRemoteBookmarksTrees.containsKey(p.getBookmarkId())).map(BookmarkDeletedModification::getBookmarkId)
				.collect(Collectors.toList());

	}

	private static class InMemoryRemoteBookmarksTree {
		private final BookmarksTree bookmarksTree;
		private final String etag;

		public InMemoryRemoteBookmarksTree(BookmarksTree bookmarksTree, String etag) {
			this.bookmarksTree = bookmarksTree;
			this.etag = etag;
		}

	}

}
