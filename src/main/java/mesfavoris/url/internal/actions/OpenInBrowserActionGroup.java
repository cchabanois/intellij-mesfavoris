package mesfavoris.url.internal.actions;

import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAware;
import mesfavoris.model.Bookmark;
import mesfavoris.url.UrlBookmarkProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OpenInBrowserActionGroup extends ActionGroup implements DumbAware {

    public OpenInBrowserActionGroup() {
        super("Open In", true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        List<WebBrowser> browsers = WebBrowserManager.getInstance().getActiveBrowsers();
        boolean visible = !browsers.isEmpty() && hasUrlBookmarkSelected(event);
        event.getPresentation().setEnabledAndVisible(visible);
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent event) {
        return WebBrowserManager.getInstance().getActiveBrowsers().stream()
                .map(OpenUrlInSpecificBrowserAction::new)
                .toArray(AnAction[]::new);
    }

    private boolean hasUrlBookmarkSelected(@NotNull AnActionEvent event) {
        Object[] items = event.getDataContext().getData(PlatformDataKeys.SELECTED_ITEMS);
        if (items == null) return false;
        for (Object item : items) {
            if (item instanceof Bookmark b && b.getPropertyValue(UrlBookmarkProperties.PROP_URL) != null) {
                return true;
            }
        }
        return false;
    }
}
