package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import mesfavoris.internal.settings.placeholders.PlaceholdersDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Action to open placeholders management from the tool window
 */
public class ManagePlaceholdersAction extends AnAction implements DumbAware {

    public ManagePlaceholdersAction() {
        super("Manage Placeholders", "Manage path placeholders with usage statistics", null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project != null) {
            PlaceholdersDialog dialog = new PlaceholdersDialog(project);
            dialog.show();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // Enable only when we have a project
        event.getPresentation().setEnabledAndVisible(event.getProject() != null);
    }
}
