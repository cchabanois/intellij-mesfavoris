package mesfavoris.ui.renderers;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.LayeredIcon;
import mesfavoris.bookmarktype.IBookmarkLabelProvider;
import mesfavoris.icons.MesFavorisIcons;
import mesfavoris.internal.ui.virtual.BookmarkLink;
import mesfavoris.internal.ui.virtual.VirtualBookmarkFolder;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksDirtyStateTracker;
import mesfavoris.remote.RemoteBookmarksStoreManager;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

import static java.util.Collections.emptyList;
import static mesfavoris.tests.commons.bookmarks.BookmarksTreeBuilder.bookmarksTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BookmarksTreeCellRendererTest extends BasePlatformTestCase {

    private BookmarksTreeCellRenderer renderer;
    private Disposable testDisposable;
    private IBookmarkLabelProvider bookmarkLabelProvider;
    private Project project;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        testDisposable = Disposer.newDisposable();
        Disposer.register(getTestRootDisposable(), testDisposable);

        project = getProject();
        BookmarksTree bookmarksTree = bookmarksTree("root").build();
        BookmarkDatabase bookmarkDatabase = new BookmarkDatabase("test", bookmarksTree);
        RemoteBookmarksStoreManager remoteBookmarksStoreManager = mock(RemoteBookmarksStoreManager.class);
        IBookmarksDirtyStateTracker bookmarksDirtyStateTracker = mock(IBookmarksDirtyStateTracker.class);
        bookmarkLabelProvider = mock(IBookmarkLabelProvider.class);

        renderer = new BookmarksTreeCellRenderer(project, bookmarkDatabase, remoteBookmarksStoreManager, bookmarksDirtyStateTracker, bookmarkLabelProvider, testDisposable);
    }

    @Test
    public void testVirtualFolderOverlay() {
        // Given
        VirtualBookmarkFolder virtualFolder = new VirtualBookmarkFolder(new BookmarkId("parent"), "Virtual Folder") {
            @Override
            public java.util.List<BookmarkLink> getChildren() {
                return emptyList();
            }

            @Override
            protected void initListening() {
            }

            @Override
            protected void stopListening() {
            }
        };
        BookmarkFolder bookmarkFolder = virtualFolder.getBookmarkFolder();
        JTree tree = new JTree();
        when(bookmarkLabelProvider.getIcon(project, bookmarkFolder)).thenReturn(MesFavorisIcons.bookmark);
        when(bookmarkLabelProvider.getStyledText(project, bookmarkFolder)).thenReturn(new StyledString("Virtual Folder"));

        // When
        renderer.customizeCellRenderer(tree, virtualFolder, false, false, false, 0, false);
        Icon icon = renderer.getIcon();

        // Then
        assertThat(icon).isInstanceOf(LayeredIcon.class);
        LayeredIcon layeredIcon = (LayeredIcon) icon;
        assertThat(layeredIcon.getAllLayers()[2]).isEqualTo(MesFavorisIcons.VIRTUAL_OVERLAY);
    }
}
