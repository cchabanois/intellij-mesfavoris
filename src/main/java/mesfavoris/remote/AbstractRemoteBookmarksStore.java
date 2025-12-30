package mesfavoris.remote;

import com.intellij.openapi.project.Project;
import mesfavoris.model.BookmarkId;

public abstract class AbstractRemoteBookmarksStore implements IRemoteBookmarksStore {
	private final Project project;
	private RemoteBookmarksStoreDescriptor descriptor;

	public AbstractRemoteBookmarksStore(Project project) {
		this.project = project;
	}

	public void init(RemoteBookmarksStoreDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	@Override
	public RemoteBookmarksStoreDescriptor getDescriptor() {
		return descriptor;
	}

	protected void postConnected() {
		project.getMessageBus().syncPublisher(RemoteBookmarksStoreListener.TOPIC)
				.remoteBookmarksStoreConnected(getDescriptor().id());
	}

	protected void postDisconnected() {
		project.getMessageBus().syncPublisher(RemoteBookmarksStoreListener.TOPIC)
				.remoteBookmarksStoreDisconnected(getDescriptor().id());
	}

	protected void postMappingAdded(BookmarkId bookmarkFolderId) {
		project.getMessageBus().syncPublisher(RemoteBookmarksStoreListener.TOPIC)
				.mappingAdded(getDescriptor().id(), bookmarkFolderId);
	}

	protected void postMappingRemoved(BookmarkId bookmarkFolderId) {
		project.getMessageBus().syncPublisher(RemoteBookmarksStoreListener.TOPIC)
				.mappingRemoved(getDescriptor().id(), bookmarkFolderId);
	}

	protected void postRemoteBookmarksTreeChanged(BookmarkId bookmarkFolderId) {
		project.getMessageBus().syncPublisher(RemoteBookmarksStoreListener.TOPIC)
				.remoteBookmarksTreeChanged(getDescriptor().id(), bookmarkFolderId);
	}

	@Override
	public void dispose() {
		// Default implementation does nothing
		// Subclasses should override if they need cleanup
	}

}
