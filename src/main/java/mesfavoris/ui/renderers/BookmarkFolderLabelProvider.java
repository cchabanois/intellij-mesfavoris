package mesfavoris.ui.renderers;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import mesfavoris.bookmarktype.AbstractBookmarkLabelProvider;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarkFolder;
import mesfavoris.remote.RemoteBookmarksStoreManager;

import javax.swing.*;
import java.util.Optional;

public class BookmarkFolderLabelProvider extends AbstractBookmarkLabelProvider {

    public BookmarkFolderLabelProvider() {
    }

    @Override
    public StyledString getStyledText(Context context, Bookmark bookmark) {
        BookmarkFolder bookmarkFolder = (BookmarkFolder) bookmark;
        StyledString result = super.getStyledText(context, bookmark);

        Project project = context.get(Context.PROJECT);
        if (project == null) {
            return result;
        }

        RemoteBookmarksStoreManager remoteBookmarksStoreManager = project.getService(RemoteBookmarksStoreManager.class);
        Optional<RemoteBookmarkFolder> remoteBookmarkFolder = remoteBookmarksStoreManager
                .getRemoteBookmarkFolder(bookmarkFolder.getId());

        // Add bookmarks count if available
        if (remoteBookmarkFolder.isPresent()) {
            Optional<Integer> bookmarksCount = getBookmarksCount(remoteBookmarkFolder.get());
            if (bookmarksCount.isPresent()) {
                SimpleTextAttributes countStyle = new SimpleTextAttributes(
                        SimpleTextAttributes.STYLE_PLAIN,
                        JBColor.YELLOW.darker()
                );
                result = result.append(String.format(" (%d)", bookmarksCount.get()), countStyle);
            }
        }

        // Add readonly indicator if connected and readonly
        Optional<IRemoteBookmarksStore> remoteBookmarksStore = remoteBookmarkFolder
                .flatMap(f -> remoteBookmarksStoreManager.getRemoteBookmarksStore(f.getRemoteBookmarkStoreId()));
        if (remoteBookmarksStore.filter(store -> store.getState() == IRemoteBookmarksStore.State.connected).isPresent()
                && remoteBookmarkFolder.filter(this::isReadOnly).isPresent()) {
            SimpleTextAttributes readonlyStyle = new SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_PLAIN,
                    JBColor.YELLOW.darker()
            );
            result = result.append(" [readonly]", readonlyStyle);
        }

        return result;
    }

    private boolean isReadOnly(RemoteBookmarkFolder remoteBookmarkFolder) {
        return Boolean.TRUE.toString()
                .equalsIgnoreCase(remoteBookmarkFolder.getProperties().get(RemoteBookmarkFolder.PROP_READONLY));
    }

    private Optional<Integer> getBookmarksCount(RemoteBookmarkFolder remoteBookmarkFolder) {
        String bookmarksCount = remoteBookmarkFolder.getProperties().get(RemoteBookmarkFolder.PROP_BOOKMARKS_COUNT);
        if (bookmarksCount == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(bookmarksCount));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public Icon getIcon(Context context, Bookmark bookmark) {
        return AllIcons.Nodes.Folder;
    }

    @Override
    public boolean canHandle(Context context, Bookmark bookmark) {
        return bookmark instanceof BookmarkFolder;
    }

}
