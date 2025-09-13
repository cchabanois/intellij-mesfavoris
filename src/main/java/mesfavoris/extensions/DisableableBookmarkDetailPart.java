package mesfavoris.extensions;

import mesfavoris.bookmarktype.IDisabledBookmarkTypesProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.ui.details.IBookmarkDetailPart;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DisableableBookmarkDetailPart implements IBookmarkDetailPart {
    private final IBookmarkDetailPart parentBookmarkDetailPart;
    private final IDisabledBookmarkTypesProvider disabledTypesProvider;
    private final String bookmarkTypeName;

    public DisableableBookmarkDetailPart(@NotNull String bookmarkTypeName, @NotNull IBookmarkDetailPart parentBookmarkDetailPart, @NotNull IDisabledBookmarkTypesProvider disabledTypesProvider) {
        this.bookmarkTypeName = bookmarkTypeName;
        this.parentBookmarkDetailPart = parentBookmarkDetailPart;
        this.disabledTypesProvider = disabledTypesProvider;
    }

    @Override
    public String getTitle() {
        return parentBookmarkDetailPart.getTitle();
    }

    @Override
    public void init() {
        parentBookmarkDetailPart.init();
    }

    @Override
    public JComponent createComponent() {
        return parentBookmarkDetailPart.createComponent();
    }

    @Override
    public void setBookmark(Bookmark bookmark) {
        parentBookmarkDetailPart.setBookmark(bookmark);
    }

    @Override
    public boolean canHandle(Bookmark bookmark) {
        if (!disabledTypesProvider.isBookmarkTypeEnabled(bookmarkTypeName)) {
            return false;
        }
        return parentBookmarkDetailPart.canHandle(bookmark);
    }

    @Override
    public void dispose() {
        parentBookmarkDetailPart.dispose();
    }
}
