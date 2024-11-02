package mesfavoris.texteditor.internal;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IBookmarkMarkerAttributesProvider;
import mesfavoris.model.Bookmark;

import java.util.HashMap;
import java.util.Map;

public class WorkspaceFileBookmarkMarkerAttributesProvider implements IBookmarkMarkerAttributesProvider {

	private final WorkspaceFileBookmarkLocationProvider workspaceFileBookmarkLocationProvider;

	public WorkspaceFileBookmarkMarkerAttributesProvider() {
		this(new WorkspaceFileBookmarkLocationProvider());
	}

	public WorkspaceFileBookmarkMarkerAttributesProvider(
			WorkspaceFileBookmarkLocationProvider textEditorBookmarkLocationProvider) {
		this.workspaceFileBookmarkLocationProvider = textEditorBookmarkLocationProvider;
	}

	@Override
	public BookmarkMarker getMarkerDescriptor(Project project, Bookmark bookmark, ProgressIndicator monitor) {
		IBookmarkLocation location = ReadAction.compute(() -> workspaceFileBookmarkLocationProvider.getBookmarkLocation(project, bookmark, monitor));
		if (!(location instanceof WorkspaceFileBookmarkLocation)) {
			return null;
		}
		WorkspaceFileBookmarkLocation workspaceFileBookmarkLocation = (WorkspaceFileBookmarkLocation) location;
		int lineNumber = workspaceFileBookmarkLocation.getLineNumber() != null ? workspaceFileBookmarkLocation.getLineNumber() : 0;
		Map<String,String> attributes = new HashMap<>();
		attributes.put(BookmarkMarker.LINE_NUMBER, Integer.toString(lineNumber + 1));
		return new BookmarkMarker(workspaceFileBookmarkLocation.getWorkspaceFile(), bookmark.getId(), attributes);
	}

}
