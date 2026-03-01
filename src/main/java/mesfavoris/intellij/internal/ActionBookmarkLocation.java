package mesfavoris.intellij.internal;

import mesfavoris.bookmarktype.IBookmarkLocation;

public class ActionBookmarkLocation implements IBookmarkLocation {
    private final String actionId;

    public ActionBookmarkLocation(String actionId) {
        this.actionId = actionId;
    }

    public String getActionId() {
        return actionId;
    }
}
