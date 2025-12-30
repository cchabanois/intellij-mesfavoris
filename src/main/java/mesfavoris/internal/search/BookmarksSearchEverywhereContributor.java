package mesfavoris.internal.search;

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.Processor;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarksTree;
import mesfavoris.service.IBookmarksService;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * Contributor for Search Everywhere dialog to search bookmarks
 */
public class BookmarksSearchEverywhereContributor implements SearchEverywhereContributor<BookmarkItem> {
    private static final Logger LOG = Logger.getInstance(BookmarksSearchEverywhereContributor.class);

    private final Project project;
    private final BookmarkDatabase bookmarkDatabase;
    private final IBookmarksService bookmarksService;

    public BookmarksSearchEverywhereContributor(@NotNull AnActionEvent event) {
        this.project = event.getProject();
        if (project != null) {
            this.bookmarksService = project.getService(IBookmarksService.class);
            this.bookmarkDatabase = bookmarksService.getBookmarkDatabase();
        } else {
            this.bookmarksService = null;
            this.bookmarkDatabase = null;
        }
    }

    @NotNull
    @Override
    public String getSearchProviderId() {
        return "BookmarksSearchEverywhereContributor";
    }

    @NotNull
    @Nls
    @Override
    public String getGroupName() {
        return "Mes Favoris";
    }

    @Override
    public int getSortWeight() {
        return 300; // After Classes (200) and Files (100)
    }

    @Override
    public boolean showInFindResults() {
        // Disabled because Find tool window doesn't support custom bookmark objects
        return false;
    }

    @Override
    public boolean isShownInSeparateTab() {
        return true; // Show as a separate tab in Search Everywhere
    }

    @Override
    public boolean isEmptyPatternSupported() {
        return true; // Allow searching with empty pattern to show all bookmarks
    }

    @Override
    public void fetchElements(@NotNull String pattern,
                              @NotNull ProgressIndicator progressIndicator,
                              @NotNull Processor<? super BookmarkItem> consumer) {
        if (bookmarkDatabase == null) {
            return;
        }

        BookmarksTree bookmarksTree = bookmarkDatabase.getBookmarksTree();

        // If pattern is empty, show all bookmarks
        if (pattern.isEmpty()) {
            collectAllBookmarks(bookmarksTree, bookmarksTree.getRootFolder(), consumer, progressIndicator);
        } else {
            String searchText = pattern.toLowerCase();
            collectMatchingBookmarks(bookmarksTree, bookmarksTree.getRootFolder(), searchText, consumer, progressIndicator);
        }
    }

    private void collectAllBookmarks(@NotNull BookmarksTree bookmarksTree,
                                     @NotNull Bookmark bookmark,
                                     @NotNull Processor<? super BookmarkItem> consumer,
                                     @NotNull ProgressIndicator progressIndicator) {
        if (progressIndicator.isCanceled()) {
            return;
        }

        // Don't add the root folder itself
        if (!(bookmark instanceof BookmarkFolder && bookmark == bookmarksTree.getRootFolder())) {
            consumer.process(new BookmarkItem(bookmarksTree, bookmark));
        }

        if (bookmark instanceof BookmarkFolder folder) {
            List<Bookmark> children = bookmarksTree.getChildren(folder.getId());
            for (Bookmark child : children) {
                collectAllBookmarks(bookmarksTree, child, consumer, progressIndicator);
            }
        }
    }

    private void collectMatchingBookmarks(@NotNull BookmarksTree bookmarksTree,
                                          @NotNull Bookmark bookmark,
                                          @NotNull String searchText,
                                          @NotNull Processor<? super BookmarkItem> consumer,
                                          @NotNull ProgressIndicator progressIndicator) {
        if (progressIndicator.isCanceled()) {
            return;
        }

        if (matchesSearchText(bookmark, searchText)) {
            consumer.process(new BookmarkItem(bookmarksTree, bookmark));
        }

        if (bookmark instanceof BookmarkFolder folder) {
            List<Bookmark> children = bookmarksTree.getChildren(folder.getId());
            for (Bookmark child : children) {
                collectMatchingBookmarks(bookmarksTree, child, searchText, consumer, progressIndicator);
            }
        }
    }

    private boolean matchesSearchText(@NotNull Bookmark bookmark, @NotNull String searchText) {
        String name = bookmark.getPropertyValue(Bookmark.PROPERTY_NAME);
        if (name != null && name.toLowerCase().contains(searchText)) {
            return true;
        }

        String comment = bookmark.getPropertyValue(Bookmark.PROPERTY_COMMENT);
        return comment != null && comment.toLowerCase().contains(searchText);
    }

    @NotNull
    @Override
    public ListCellRenderer<? super BookmarkItem> getElementsRenderer() {
        return new BookmarksSearchEverywhereRenderer();
    }

    @Nullable
    @Override
    public Object getDataForItem(@NotNull BookmarkItem element, @NotNull String dataId) {
        // Provide bookmark data for the Find tool window
        if (PlatformDataKeys.SELECTED_ITEM.is(dataId)) {
            return element.getBookmark();
        }
        if (PlatformDataKeys.SELECTED_ITEMS.is(dataId)) {
            return new Object[]{element.getBookmark()};
        }
        return null;
    }

    @Override
    public boolean processSelectedItem(@NotNull BookmarkItem selected, int modifiers, @NotNull String searchText) {
        if (project == null || bookmarksService == null) {
            return false;
        }

        // Navigate to the bookmark in a background thread to avoid EDT violations
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                bookmarksService.gotoBookmark(selected.getBookmark().getId(), new EmptyProgressIndicator());
            } catch (Exception e) {
                LOG.error("Failed to navigate to bookmark", e);
            }
        });

        return true;
    }

    public static class Factory implements SearchEverywhereContributorFactory<BookmarkItem> {
        @NotNull
        @Override
        public SearchEverywhereContributor<BookmarkItem> createContributor(@NotNull AnActionEvent initEvent) {
            LOG.info("BookmarksSearchEverywhereContributor.Factory.createContributor called");
            return new BookmarksSearchEverywhereContributor(initEvent);
        }
    }
}

