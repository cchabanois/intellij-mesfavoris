package mesfavoris.texteditor.internal;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.bookmarktype.IBookmarkMarkerAttributesProvider;
import mesfavoris.bookmarktype.IFileBookmarkLocation;
import mesfavoris.model.Bookmark;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
	public BookmarkMarker getMarkerDescriptor(Project project, Bookmark bookmark, Optional<IFileBookmarkLocation> fileBookmarkLocation, ProgressIndicator monitor) {
		IFileBookmarkLocation location = fileBookmarkLocation.orElseGet(() -> ReadAction.compute(() -> workspaceFileBookmarkLocationProvider.getBookmarkLocation(project, bookmark, monitor)));
		if (location == null) {
			return null;
		}
		int lineNumber = location.getLineNumber() != null ? location.getLineNumber() : 0;
		Map<String,String> attributes = new HashMap<>();
		attributes.put(BookmarkMarker.LINE_NUMBER, Integer.toString(lineNumber));
		return new BookmarkMarker(location.getFile(), bookmark.getId(), attributes);
	}

}
