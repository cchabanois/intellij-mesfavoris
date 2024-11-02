package mesfavoris.bookmarktype;

import java.util.function.Supplier;

import com.intellij.openapi.project.Project;
import mesfavoris.bookmarktype.IBookmarkLabelProvider.Context;
import mesfavoris.model.BookmarkDatabase;
import mesfavoris.model.BookmarksTree;

public class BookmarkDatabaseLabelProviderContext implements Context {
    private final String bookmarkDatabaseId;
    private final Supplier<BookmarksTree> bookmarksTreeProvider;
    private final Project project;

    public BookmarkDatabaseLabelProviderContext(Project project, BookmarkDatabase bookmarkDatabase) {
        this(project, bookmarkDatabase.getId(), () -> bookmarkDatabase.getBookmarksTree());
    }

    public BookmarkDatabaseLabelProviderContext(Project project, String bookmarkDatabaseId, Supplier<BookmarksTree> bookmarksTreeProvider) {
        this.project = project;
        this.bookmarkDatabaseId = bookmarkDatabaseId;
        this.bookmarksTreeProvider = bookmarksTreeProvider;
    }

    @Override
    public <T> T get(String name) {
        switch (name) {
            case Context.PROJECT:
                return (T) project;
            case Context.BOOKMARK_DATABASE_ID:
                return (T) bookmarkDatabaseId;
            case Context.BOOKMARKS_TREE:
                return (T) bookmarksTreeProvider.get();
            default:
                return null;
        }
    }

}
