package mesfavoris.remote;

import com.intellij.openapi.project.Project;
import mesfavoris.model.BookmarkId;

public abstract class AbstractRemoteBookmarksStore implements IRemoteBookmarksStore {
	private final Project project;
	private IRemoteBookmarksStoreDescriptor descriptor;

	public AbstractRemoteBookmarksStore(Project project) {
		this.project = project;
	}

	public void init(IRemoteBookmarksStoreDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	@Override
	public IRemoteBookmarksStoreDescriptor getDescriptor() {
		return descriptor;
	}

	protected void postConnected() {
		project.getMessageBus().syncPublisher(RemoteBookmarksStoreListener.TOPIC)
				.remoteBookmarksStoreConnected(getDescriptor().getId());
	}

	protected void postDisconnected() {
		project.getMessageBus().syncPublisher(RemoteBookmarksStoreListener.TOPIC)
				.remoteBookmarksStoreDisconnected(getDescriptor().getId());
	}

	protected void postMappingAdded(BookmarkId bookmarkFolderId) {
		project.getMessageBus().syncPublisher(RemoteBookmarksStoreListener.TOPIC)
				.mappingAdded(getDescriptor().getId(), bookmarkFolderId);
	}

	protected void postMappingRemoved(BookmarkId bookmarkFolderId) {
		project.getMessageBus().syncPublisher(RemoteBookmarksStoreListener.TOPIC)
				.mappingRemoved(getDescriptor().getId(), bookmarkFolderId);
	}

	protected void postRemoteBookmarksTreeChanged(BookmarkId bookmarkFolderId) {
		project.getMessageBus().syncPublisher(RemoteBookmarksStoreListener.TOPIC)
				.remoteBookmarksTreeChanged(getDescriptor().getId(), bookmarkFolderId);
	}

}
