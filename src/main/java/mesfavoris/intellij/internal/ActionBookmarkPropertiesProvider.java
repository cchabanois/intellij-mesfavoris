package mesfavoris.intellij.internal;

import com.intellij.ide.util.gotoByName.GotoActionModel;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.bookmarktype.AbstractBookmarkPropertiesProvider;
import mesfavoris.intellij.IntellijBookmarkProperties;

import java.util.Map;

import static mesfavoris.texteditor.TextEditorBookmarkProperties.PROPERTY_NAME;

public class ActionBookmarkPropertiesProvider extends AbstractBookmarkPropertiesProvider  {
    @Override
    public void addBookmarkProperties(Map<String, String> bookmarkProperties, DataContext dataContext, ProgressIndicator progress) {
        Object selectedItem = dataContext.getData(PlatformDataKeys.SELECTED_ITEM);

        if (selectedItem instanceof GotoActionModel.MatchedValue matchedValue && matchedValue.value instanceof GotoActionModel.ActionWrapper actionWrapper) {
            ActionManager actionManager = ActionManager.getInstance();

            String actionId = actionManager.getId(actionWrapper.getAction());
            String text = actionWrapper.getAction().getTemplatePresentation().getText();

            putIfAbsent(bookmarkProperties, PROPERTY_NAME, text);
            putIfAbsent(bookmarkProperties, IntellijBookmarkProperties.PROP_ACTION_ID, actionId);
        }

    }
}
