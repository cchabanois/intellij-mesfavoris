package mesfavoris.intellij.internal;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import mesfavoris.bookmarktype.AbstractBookmarkLabelProvider;
import mesfavoris.intellij.IntellijBookmarkProperties;
import mesfavoris.model.Bookmark;
import mesfavoris.ui.renderers.StyledString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class ActionBookmarkLabelProvider extends AbstractBookmarkLabelProvider {
    @Override
    public Icon getIcon(@Nullable Project project, @NotNull Bookmark bookmark) {
        String actionId = bookmark.getPropertyValue(IntellijBookmarkProperties.PROP_ACTION_ID);
        if (actionId != null) {
            AnAction action = ActionManager.getInstance().getAction(actionId);
            if (action != null) {
                Icon icon = action.getTemplatePresentation().getIcon();
                return Objects.requireNonNullElse(icon, AllIcons.Actions.Execute);
            }
        }
        return null;
    }

    @Override
    public StyledString getStyledText(@Nullable Project project, @NotNull Bookmark bookmark) {
        String actionId = bookmark.getPropertyValue(IntellijBookmarkProperties.PROP_ACTION_ID);
        if (actionId != null) {
            AnAction action = ActionManager.getInstance().getAction(actionId);
            if (action != null) {
                String actionText = action.getTemplatePresentation().getText();
                StyledString styledString = (actionText != null && !actionText.isEmpty()) ? new StyledString(actionText) : super.getStyledText(project, bookmark);
                String shortcutText = KeymapUtil.getFirstKeyboardShortcutText(action);
                if (!shortcutText.isEmpty()) {
                    styledString = styledString.append(" (" + shortcutText + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
                return styledString;
            }
        }
        return super.getStyledText(project, bookmark);
    }

    @Override
    public boolean canHandle(@Nullable Project project, @NotNull Bookmark bookmark) {
        return bookmark.getPropertyValue(IntellijBookmarkProperties.PROP_ACTION_ID) != null;
    }
}
