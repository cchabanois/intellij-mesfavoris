package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import mesfavoris.BookmarksException;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.NotNull;

/**
 * Action to refresh remote bookmark folders from all connected stores
 */
public class RefreshRemoteFoldersAction extends AbstractBookmarkAction implements DumbAware {
	private static final Logger LOG = Logger.getInstance(RefreshRemoteFoldersAction.class);

	public RefreshRemoteFoldersAction() {
		getTemplatePresentation().setText("Refresh Remote Folders");
		getTemplatePresentation().setDescription("Refresh bookmarks from all connected remote stores");
	}

	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		return ActionUpdateThread.BGT;
	}

	@Override
	public void update(@NotNull AnActionEvent event) {
		// Enable only when we have a project
		event.getPresentation().setEnabledAndVisible(event.getProject() != null);
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent event) {
		Project project = event.getProject();
		if (project == null) {
			return;
		}

		IBookmarksService bookmarksService = getBookmarksService(event);

		ProgressManager.getInstance().run(new Task.Backgroundable(project, "Refreshing Remote Bookmark Folders", true) {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				try {
					bookmarksService.refresh(indicator);
				} catch (BookmarksException e) {
					LOG.error("Failed to refresh remote bookmark folders", e);
					ApplicationManager.getApplication().invokeLater(() ->
							Messages.showMessageDialog(
									project,
									e.getMessage(),
									"Could Not Refresh Remote Folders",
									Messages.getErrorIcon()));
				}
			}
		});
	}
}
