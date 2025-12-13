package mesfavoris.internal.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

/**
 * Action group for settings menu
 */
public class SettingsActionGroup extends DefaultActionGroup implements DumbAware {
    private final SettingsAction settingsAction;

    public SettingsActionGroup() {
        super("Settings", true);
        this.settingsAction = new SettingsAction();
        getTemplatePresentation().setPopupGroup(true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public AnAction @NotNull [] getChildren(@NotNull AnActionEvent event) {
        return new AnAction[] { settingsAction };
    }
}

