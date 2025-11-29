package mesfavoris.gdrive.decoration;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import mesfavoris.gdrive.mappings.BookmarkMapping;
import mesfavoris.gdrive.mappings.BookmarkMappingsStore;
import mesfavoris.internal.workspace.BookmarksWorkspaceFactory;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.ui.renderers.BookmarkFolderLabelProvider;
import mesfavoris.ui.renderers.StyledString;

import java.util.Optional;

public class GDriveBookmarkFolderLabelProvider extends BookmarkFolderLabelProvider {

	public GDriveBookmarkFolderLabelProvider() {
	}

	@Override
	public StyledString getStyledText(Context context, Bookmark bookmark) {
		StyledString styledString = super.getStyledText(context, bookmark);

		Project project = context.get(Context.PROJECT);
		if (project == null) {
			return styledString;
		}

		BookmarkMappingsStore bookmarkMappings = project.getService(BookmarkMappingsStore.class);
		BookmarkFolder bookmarkFolder = (BookmarkFolder) bookmark;
		BookmarkId bookmarkId = bookmarkFolder.getId();
		Optional<BookmarkMapping> bookmarkMapping = bookmarkMappings.getMapping(bookmarkId);
		if (bookmarkMapping.isEmpty()) {
			return styledString;
		}

		String sharingUser = bookmarkMapping.get().getProperties().get(BookmarkMapping.PROP_SHARING_USER);
		if (sharingUser == null) {
			return styledString;
		}

		SimpleTextAttributes sharingStyle = new SimpleTextAttributes(
				SimpleTextAttributes.STYLE_PLAIN,
				JBColor.YELLOW.darker()
		);
		return styledString.append(String.format(" [Shared by %s]", sharingUser), sharingStyle);
	}

	@Override
	public boolean canHandle(Context context, Bookmark bookmark) {
		if (!super.canHandle(context, bookmark) || !isMainBookmarkDatabase(context)) {
			return false;
		}

		Project project = context.get(Context.PROJECT);
		if (project == null) {
			return false;
		}

		BookmarkMappingsStore bookmarkMappings = project.getService(BookmarkMappingsStore.class);
		return bookmarkMappings.getMapping(bookmark.getId()).isPresent();
	}

	private boolean isMainBookmarkDatabase(Context context) {
		String bookmarkDatabaseId = context.get(Context.BOOKMARK_DATABASE_ID);
		return BookmarksWorkspaceFactory.BOOKMARKS_DATABASE_ID.equals(bookmarkDatabaseId);
	}

}
