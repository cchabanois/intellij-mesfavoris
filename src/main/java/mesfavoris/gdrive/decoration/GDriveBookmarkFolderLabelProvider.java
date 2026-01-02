package mesfavoris.gdrive.decoration;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import mesfavoris.gdrive.mappings.BookmarkMapping;
import mesfavoris.gdrive.mappings.BookmarkMappingsStore;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.ui.renderers.BookmarkFolderLabelProvider;
import mesfavoris.ui.renderers.StyledString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class GDriveBookmarkFolderLabelProvider extends BookmarkFolderLabelProvider {

	public GDriveBookmarkFolderLabelProvider() {
	}

	@Override
	public StyledString getStyledText(@Nullable Project project, @NotNull Bookmark bookmark) {
		StyledString styledString = super.getStyledText(project, bookmark);

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
	public boolean canHandle(@Nullable Project project, @NotNull Bookmark bookmark) {
		if (!super.canHandle(project, bookmark)) {
			return false;
		}

		if (project == null) {
			return false;
		}

		BookmarkMappingsStore bookmarkMappings = project.getService(BookmarkMappingsStore.class);
		return bookmarkMappings.getMapping(bookmark.getId()).isPresent();
	}

}
