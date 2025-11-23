package mesfavoris.ui.renderers;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import mesfavoris.bookmarktype.BookmarkDatabaseLabelProviderContext;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.commons.Adapters;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkId;
import mesfavoris.persistence.IBookmarksDirtyStateListener;
import mesfavoris.persistence.IBookmarksDirtyStateTracker;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

import static mesfavoris.remote.IRemoteBookmarksStore.State.connected;

public class BookmarksTreeCellRenderer extends ColoredTreeCellRenderer implements Disposable {
    private final BookmarkDatabase bookmarkDatabase;
    private final IBookmarkLabelProvider bookmarkLabelProvider;
    private final IBookmarksDirtyStateTracker bookmarksDirtyStateTracker;
    private final BookmarkDatabaseLabelProviderContext context;
    private final Color commentColor = new JBColor(new Color(63, 127, 95), new Color(63, 127, 95));
    private final IBookmarksDirtyStateListener dirtyStateListener = dirtyBookmarks -> ApplicationManager.getApplication().invokeLater(() -> {
        if (!getTree().isShowing()) {
            return;
        }
        getTree().repaint();
    });
    private final RemoteBookmarksStoreManager remoteBookmarksStoreManager;

    public BookmarksTreeCellRenderer(Project project, BookmarkDatabase bookmarkDatabase, RemoteBookmarksStoreManager remoteBookmarksStoreManager, IBookmarksDirtyStateTracker bookmarksDirtyStateTracker, IBookmarkLabelProvider bookmarkLabelProvider, Disposable parentDisposable) {
        this.bookmarkDatabase = bookmarkDatabase;
        this.remoteBookmarksStoreManager = remoteBookmarksStoreManager;
        this.bookmarkLabelProvider = bookmarkLabelProvider;
        this.bookmarksDirtyStateTracker = bookmarksDirtyStateTracker;
        this.context = new BookmarkDatabaseLabelProviderContext(project, bookmarkDatabase);
        bookmarksDirtyStateTracker.addListener(dirtyStateListener);

        Disposer.register(parentDisposable, this);
    }

    @Override
    public void dispose() {
        bookmarksDirtyStateTracker.removeListener(dirtyStateListener);
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        StyledString styledText = getStyledText(value);
        styledText.appendTo(this);
        Icon icon = getIcon(value);
        setIcon(icon);
    }

    private Icon getIcon(final Object element) {
        Bookmark bookmark = Adapters.adapt(element, Bookmark.class);
        Icon baseIcon = bookmarkLabelProvider.getIcon(context, bookmark);
        if (baseIcon == null) {
            return null;
        }

        // Create layered icon with base icon and 4 overlay layers
        LayeredIcon layeredIcon = new LayeredIcon(5);
        layeredIcon.setIcon(baseIcon, 0);

        // Add overlay icons
        addOverlayIcons(element, layeredIcon);

        return layeredIcon;
    }

    private StyledString getStyledText(final Object element) {
        Bookmark bookmark = Adapters.adapt(element, Bookmark.class);
        String comment = getFirstCommentLine(bookmark);
        boolean hasComment = comment != null && !comment.trim().isEmpty();
        boolean isDisabled = isUnderDisconnectedRemoteBookmarkFolder(bookmark);
        StyledString styledString = new StyledString();
        if (bookmarksDirtyStateTracker.getDirtyBookmarks().contains(bookmark.getId())) {
            styledString = styledString.append("> ");
        }
        styledString = styledString.append(bookmarkLabelProvider.getStyledText(context, bookmark));
        if (isDisabled) {
            styledString = styledString.setStyle(SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        if (hasComment) {
            Color color = commentColor;
            Font font = null;
            if (isDisabled) {
                color = UIUtil.getInactiveTextColor();
            }
            styledString = styledString.append(" - " + comment, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color));
        }
        return styledString;
    }

    private String getFirstCommentLine(Bookmark bookmark) {
        String comment = bookmark.getPropertyValue(Bookmark.PROPERTY_COMMENT);
        if (comment == null) {
            return null;
        }
        try (Scanner scanner = new Scanner(comment)) {
            return scanner.nextLine();
        } catch (NoSuchElementException e) {
            return null;
        }
    }


    private void addOverlayIcons(final Object element, LayeredIcon layeredIcon) {
        Bookmark bookmark = Adapters.adapt(element, Bookmark.class);
        if (bookmark == null) {
            return;
        }

        // Add remote bookmark store overlay icon (top-right position)
        getRemoteBookmarkStore(bookmark.getId())
                .map(remoteBookmarksStore -> remoteBookmarksStore.getDescriptor().iconOverlay())
                .ifPresent(icon -> layeredIcon.setIcon(icon, 1, SwingConstants.NORTH_EAST));
    }

    private Optional<IRemoteBookmarksStore> getRemoteBookmarkStore(BookmarkId bookmarkFolderId) {
        return remoteBookmarksStoreManager.getRemoteBookmarkFolder(bookmarkFolderId)
                .flatMap(f -> remoteBookmarksStoreManager.getRemoteBookmarksStore(f.getRemoteBookmarkStoreId()));
    }


    private boolean isUnderDisconnectedRemoteBookmarkFolder(Bookmark bookmark) {
        return remoteBookmarksStoreManager
                .getRemoteBookmarkFolderContaining(bookmarkDatabase.getBookmarksTree(), bookmark.getId())
                .flatMap(remoteBookmarkFolder -> remoteBookmarksStoreManager
                        .getRemoteBookmarksStore(remoteBookmarkFolder.getRemoteBookmarkStoreId()))
                .map(remoteBookmarksStore -> remoteBookmarksStore.getState() != connected).orElse(false);
    }

}
