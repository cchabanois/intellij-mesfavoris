package mesfavoris.internal.service.operations.utils;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;

import com.google.common.collect.Lists;

import mesfavoris.BookmarksException;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import static mesfavoris.internal.Constants.DEFAULT_BOOKMARKFOLDER_ID;
import com.intellij.openapi.diagnostic.Logger;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides the position where to add a new bookmark.
 * 
 * If bookmarks view is opened, the selected bookmark is used to determine the
 * position, otherwise the "default" bookmark folder is returned.
 * 
 * @author cchabanois
 *
 */
public class NewBookmarkPositionProvider implements INewBookmarkPositionProvider {
	private static final Logger LOG = Logger.getInstance(NewBookmarkPositionProvider.class);

	private final BookmarkDatabase bookmarkDatabase;
	private final Project project;
	public NewBookmarkPositionProvider(Project project, BookmarkDatabase bookmarkDatabase) {
		this.bookmarkDatabase = bookmarkDatabase;
		this.project = project;
	}

	@Override
	public NewBookmarkPosition getNewBookmarkPosition() {
		NewBookmarkPosition bookmarkPosition = getCurrentBookmarkPositionFromBookmarksToolWindow();
		if (bookmarkPosition != null && isModifiable(bookmarkPosition.getParentBookmarkId())) {
			return bookmarkPosition;
		}
		if (!exists(DEFAULT_BOOKMARKFOLDER_ID)) {
			createDefaultBookmarkFolder();
		}
		if (exists(DEFAULT_BOOKMARKFOLDER_ID) && isModifiable(DEFAULT_BOOKMARKFOLDER_ID)) {
			return new NewBookmarkPosition(DEFAULT_BOOKMARKFOLDER_ID);
		}
		return new NewBookmarkPosition(bookmarkDatabase.getBookmarksTree().getRootFolder().getId());
	}

	private void createDefaultBookmarkFolder() {
		try {
			bookmarkDatabase.modify(bookmarksTreeModifier -> {
				BookmarkFolder bookmarkFolder = new BookmarkFolder(DEFAULT_BOOKMARKFOLDER_ID, "default");
				bookmarksTreeModifier.addBookmarksAfter(bookmarksTreeModifier.getCurrentTree().getRootFolder().getId(),
						null, Lists.newArrayList(bookmarkFolder));
			});
		} catch (BookmarksException e) {
			LOG.warn("Could not create default folder", e);
		}
	}

	private boolean exists(BookmarkId bookmarkId) {
		return bookmarkDatabase.getBookmarksTree().getBookmark(bookmarkId) != null;
	}

	private boolean isModifiable(BookmarkId bookmarkId) {
		return bookmarkDatabase.getBookmarksModificationValidator()
				.validateModification(bookmarkDatabase.getBookmarksTree(), bookmarkId).isOk();
	}

	private NewBookmarkPosition getCurrentBookmarkPositionFromBookmarksToolWindow() {
		List<Bookmark> selection = getBookmarksToolWindowSelection();
		if (selection.isEmpty()) {
			return null;
		}
		Bookmark firstSelectedElement = selection.get(0);
		if (firstSelectedElement instanceof BookmarkFolder bookmarkFolder) {
            return new NewBookmarkPosition(bookmarkFolder.getId());
		} else if (firstSelectedElement != null) {
			Bookmark bookmark = firstSelectedElement;
			BookmarkFolder parentBookmark = bookmarkDatabase.getBookmarksTree().getParentBookmark(bookmark.getId());
			return new NewBookmarkPosition(parentBookmark.getId(), bookmark.getId());
		}
		return null;
	}

	private List<Bookmark> getBookmarksToolWindowSelection() {
		ToolWindow bookmarksToolWindow = ToolWindowManager.getInstance(project).getToolWindow("mesfavoris");
		Content content = bookmarksToolWindow.getContentManager().getContent(0);
		Component component = content.getComponent();
		Object[] selectedItems = getSelectedItemsFromDataContext(component);
		if (selectedItems == null) {
			return List.of();
		}
		return Arrays.stream(selectedItems).map(object -> (Bookmark)object).toList();
	}

	private Object[] getSelectedItemsFromDataContext(Component component) {
		AtomicReference<Object[]> results = new AtomicReference<>();
		ApplicationManager.getApplication().invokeAndWait(() -> ReadAction.run(() -> {
			DataContext dataContext = DataManager.getInstance().getDataContext(component);
			Object[] objects = dataContext.getData(PlatformDataKeys.SELECTED_ITEMS);
			results.set(objects);
        }));
		return results.get();
	}


}
