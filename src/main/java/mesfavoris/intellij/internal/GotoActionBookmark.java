package mesfavoris.intellij.internal;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLocation;
import mesfavoris.bookmarktype.IGotoBookmark;
import mesfavoris.model.Bookmark;

public class GotoActionBookmark implements IGotoBookmark {
    @Override
    public boolean gotoBookmark(Project project, Bookmark bookmark, IBookmarkLocation location) {
        if (location instanceof ActionBookmarkLocation actionBookmarkLocation) {
            String actionId = actionBookmarkLocation.getActionId();
            AnAction action = ActionManager.getInstance().getAction(actionId);
            if (action != null) {
                // We must run on the EDT to get the editor and invoke the action
                ApplicationManager.getApplication().invokeLater(() -> {
                    Editor currentEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                    DataContext dataContext;

                    if (currentEditor != null) {
                        // Get data context from the currently active editor
                        dataContext = DataManager.getInstance().getDataContext(currentEditor.getComponent());
                    } else {
                        // Fallback if no editor is focused, provide at least the project
                        dataContext = SimpleDataContext.builder()
                                .add(CommonDataKeys.PROJECT, project)
                                .build();
                    }

                    ActionUtil.invokeAction(action, dataContext, ActionPlaces.UNKNOWN, null, null);
                });
                return true;
            }
        }
        return false;
    }
}
