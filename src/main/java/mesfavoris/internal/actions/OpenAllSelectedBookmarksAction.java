package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import mesfavoris.BookmarksException;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Action to open all selected bookmarks
 *
 */
public class OpenAllSelectedBookmarksAction extends AbstractBookmarkAction {

	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		return ActionUpdateThread.BGT;
	}

	@Override
	public void update(@NotNull AnActionEvent event) {
		List<Bookmark> bookmarks = getSelectedBookmarks(event);
		boolean hasBookmarks = !bookmarks.isEmpty();

		// Only enable when we have bookmarks selected and focus is on the tree
		boolean focusOnTree = isFocusOnBookmarksTree();
		event.getPresentation().setEnabledAndVisible(hasBookmarks && focusOnTree);
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent event) {
		// Get all selected bookmarks recursively, excluding folders
		Set<Bookmark> bookmarks = getSelectedBookmarksRecursively(event,
				b -> !(b instanceof BookmarkFolder));

		if (bookmarks.isEmpty()) {
			return;
		}

		IBookmarksService bookmarksService = getBookmarksService(event);

		ProgressManager.getInstance().run(new Task.Backgroundable(event.getProject(), "Opening Bookmarks", true) {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				indicator.setIndeterminate(false);
				List<String> errors = new ArrayList<>();
				int current = 0;

				for (Bookmark bookmark : bookmarks) {
					if (indicator.isCanceled()) {
						break;
					}

					indicator.setFraction((double) current / bookmarks.size());
					indicator.setText("Opening bookmark: " + bookmark.getPropertyValue(Bookmark.PROPERTY_NAME));

					try {
						bookmarksService.gotoBookmark(bookmark.getId(), indicator);
					} catch (BookmarksException e) {
						errors.add(String.format("Could not open bookmark '%s': %s",
								bookmark.getPropertyValue(Bookmark.PROPERTY_NAME),
								e.getMessage()));
					}
					current++;
				}

				// Show errors if any occurred
				if (!errors.isEmpty()) {
					ApplicationManager.getApplication().invokeLater(() -> {
						String message = String.join("\n", errors);
						Messages.showErrorDialog(event.getProject(), message, "Could Not Open Some Bookmarks");
					});
				}
			}
		});
	}

}
