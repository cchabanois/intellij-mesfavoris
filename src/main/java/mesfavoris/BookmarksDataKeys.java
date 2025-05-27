package mesfavoris;

import com.intellij.openapi.actionSystem.DataKey;

import java.awt.datatransfer.Transferable;

public class BookmarksDataKeys {
    public static final DataKey<Transferable> TRANSFERABLE_DATA_KEY = DataKey.create("transferable");
    public static final DataKey<String> BOOKMARK_TYPE_DATA_KEY = DataKey.create("bookmarkType");
}
