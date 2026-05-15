package mesfavoris.url.internal.actions;

import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import mesfavoris.model.Bookmark;
import mesfavoris.url.UrlBookmarkProperties;
import org.jetbrains.annotations.NotNull;

public class OpenUrlInSpecificBrowserAction extends DumbAwareAction {

    private final WebBrowser browser;

    public OpenUrlInSpecificBrowserAction(@NotNull WebBrowser browser) {
        this.browser = browser;
        getTemplatePresentation().setText(browser.getName());
        getTemplatePresentation().setIcon(browser.getIcon());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Object[] items = event.getDataContext().getData(PlatformDataKeys.SELECTED_ITEMS);
        if (items == null) return;
        for (Object item : items) {
            if (!(item instanceof Bookmark bookmark)) continue;
            String urlStr = bookmark.getPropertyValue(UrlBookmarkProperties.PROP_URL);
            if (urlStr == null) continue;
            BrowserLauncher.getInstance().browse(urlStr, browser, event.getProject());
        }
    }
}
