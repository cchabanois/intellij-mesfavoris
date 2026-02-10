package mesfavoris.internal.actions;

import com.intellij.codeInsight.hints.InlayHintsSettings;
import com.intellij.codeInsight.hints.SettingsKey;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.FileContentUtil;
import mesfavoris.internal.markers.inlay.BookmarkCommentInlayHintsProvider;
import mesfavoris.internal.markers.inlay.BookmarkCommentInlayHintsSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Toggle action to enable/disable bookmark comment inlay hints
 */
@SuppressWarnings({"UnstableApiUsage", "UnstableApiUsage"})
public class ToggleBookmarkCommentInlayHintsAction extends ToggleAction {
    private static final SettingsKey<BookmarkCommentInlayHintsSettings> KEY = BookmarkCommentInlayHintsProvider.KEY;

    public ToggleBookmarkCommentInlayHintsAction() {
        super("Show Favori Comments in Editor");
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return InlayHintsSettings.instance().findSettings(KEY, Language.ANY, () -> new BookmarkCommentInlayHintsSettings(true)).enabled();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        InlayHintsSettings.instance().storeSettings(KEY, Language.ANY, new BookmarkCommentInlayHintsSettings(state));

        Project project = e.getProject();
        if (project != null) {
            FileContentUtil.reparseOpenedFiles();
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
