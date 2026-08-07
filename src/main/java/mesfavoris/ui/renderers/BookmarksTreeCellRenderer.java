package mesfavoris.ui.renderers;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.commons.Adapters;
import mesfavoris.icons.MesFavorisIcons;
import mesfavoris.internal.mcp.McpBookmarkProperties;
import mesfavoris.internal.ui.virtual.VirtualBookmarkFolder;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.persistence.IBookmarksDirtyStateListener;
import mesfavoris.persistence.IBookmarksDirtyStateTracker;
import mesfavoris.remote.IRemoteBookmarksStore;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import mesfavoris.tags.Tags;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

import static mesfavoris.remote.IRemoteBookmarksStore.State.connected;

public class BookmarksTreeCellRenderer extends ColoredTreeCellRenderer implements Disposable {
    private final Project project;
    private final BookmarkDatabase bookmarkDatabase;
    private final IBookmarkLabelProvider bookmarkLabelProvider;
    private final IBookmarksDirtyStateTracker bookmarksDirtyStateTracker;
    private final Color commentColor = new JBColor(new Color(63, 127, 95), new Color(63, 127, 95));
    private static final Color AI_BADGE_COLOR = new JBColor(new Color(155, 89, 214), new Color(176, 127, 232));
    // discreet grey tags: grey text with a thin grey outline, no fill
    private static final Color TAG_FG_COLOR = new JBColor(new Color(120, 120, 120), new Color(140, 140, 140));
    private static final Color TAG_BORDER_COLOR = new JBColor(new Color(180, 180, 180), new Color(90, 90, 90));
    private final IBookmarksDirtyStateListener dirtyStateListener = dirtyBookmarks -> ApplicationManager.getApplication().invokeLater(() -> {
        JTree tree = getTree();
        if (tree == null || !tree.isShowing()) {
            return;
        }
        tree.repaint();
    });
    private final RemoteBookmarksStoreManager remoteBookmarksStoreManager;

    public BookmarksTreeCellRenderer(Project project, BookmarkDatabase bookmarkDatabase, RemoteBookmarksStoreManager remoteBookmarksStoreManager, IBookmarksDirtyStateTracker bookmarksDirtyStateTracker, IBookmarkLabelProvider bookmarkLabelProvider, Disposable parentDisposable) {
        this.project = project;
        this.bookmarkDatabase = bookmarkDatabase;
        this.remoteBookmarksStoreManager = remoteBookmarksStoreManager;
        this.bookmarkLabelProvider = bookmarkLabelProvider;
        this.bookmarksDirtyStateTracker = bookmarksDirtyStateTracker;
        project.getMessageBus().connect(this)
                .subscribe(IBookmarksDirtyStateListener.TOPIC, dirtyStateListener);

        Disposer.register(parentDisposable, this);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        StyledString styledText = getStyledText(value);
        styledText.appendTo(this);
        Icon icon = getIcon(value);
        setIcon(icon);
    }

    @Override
    protected void doPaintFragmentBackground(@NotNull Graphics2D g, int index, @NotNull Color bgColor, int x, int y, int width, int height) {
        // draw tag fragments as rounded "pills" instead of the default rectangle
        Object oldAntialiasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            int verticalInset = JBUI.scale(3);
            int pillHeight = height - 2 * verticalInset;
            int top = y + verticalInset;
            int arc = JBUI.scale(9);
            // outline only (no fill) for a discreet look
            g.setColor(TAG_BORDER_COLOR);
            g.drawRoundRect(x, top, width - 1, pillHeight - 1, arc, arc);
        } finally {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    oldAntialiasing != null ? oldAntialiasing : RenderingHints.VALUE_ANTIALIAS_DEFAULT);
        }
    }

    private Icon getIcon(final Object element) {
        Bookmark bookmark = Adapters.adapt(element, Bookmark.class);
        Icon baseIcon = bookmarkLabelProvider.getIcon(project, bookmark);
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
        styledString = styledString.append(bookmarkLabelProvider.getStyledText(project, bookmark));
        if (isDisabled) {
            styledString = styledString.setStyle(SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        if (!(bookmark instanceof BookmarkFolder)
                && McpBookmarkProperties.ORIGIN_MCP.equals(bookmark.getPropertyValue(McpBookmarkProperties.PROPERTY_ORIGIN))) {
            Color badgeColor = isDisabled ? UIUtil.getInactiveTextColor() : AI_BADGE_COLOR;
            styledString = styledString.append(" [AI]", new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, badgeColor));
        }

        if (!(bookmark instanceof BookmarkFolder)) {
            Color tagFg = isDisabled ? UIUtil.getInactiveTextColor() : TAG_FG_COLOR;
            // STYLE_OPAQUE (with a non-null bg) is required so SimpleColoredComponent invokes
            // doPaintFragmentBackground, where we draw the outline; the bg itself is not filled.
            SimpleTextAttributes tagAttributes = new SimpleTextAttributes(TAG_BORDER_COLOR, tagFg, null,
                    SimpleTextAttributes.STYLE_SMALLER | SimpleTextAttributes.STYLE_OPAQUE);
            for (String tag : Tags.getTags(bookmark)) {
                styledString = styledString.append(" ");
                styledString = styledString.append(" " + tag + " ", tagAttributes);
            }
        }

        if (hasComment) {
            Color color = commentColor;
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

        // Add virtual folder overlay icon (top-left position)
        if (element instanceof VirtualBookmarkFolder) {
            layeredIcon.setIcon(MesFavorisIcons.VIRTUAL_OVERLAY, 2, SwingConstants.NORTH_WEST);
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
