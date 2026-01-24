package mesfavoris.internal.toolwindow;

import mesfavoris.commons.IAdaptable;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkFolder;
import mesfavoris.model.BookmarkId;

/**
 * Mutable wrapper for immutable Bookmark objects used in the tree model.
 * This allows the tree to maintain stable TreePath references while the underlying
 * Bookmark instances are replaced when modified.
 */
public class BookmarkTreeNode implements IAdaptable {
    private Bookmark bookmark;

    public BookmarkTreeNode(Bookmark bookmark) {
        if (bookmark == null) {
            throw new IllegalArgumentException("Bookmark cannot be null");
        }
        this.bookmark = bookmark;
    }

    /**
     * Update the wrapped bookmark instance.
     * This is called when the bookmark is modified in the database.
     */
    public void updateBookmark(Bookmark newBookmark) {
        if (newBookmark == null) {
            throw new IllegalArgumentException("Bookmark cannot be null");
        }
        if (!newBookmark.getId().equals(this.bookmark.getId())) {
            throw new IllegalArgumentException("Cannot change bookmark ID");
        }
        this.bookmark = newBookmark;
    }

    public Bookmark getBookmark() {
        return bookmark;
    }

    public BookmarkId getId() {
        return bookmark.getId();
    }

    public boolean isFolder() {
        return bookmark instanceof BookmarkFolder;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BookmarkTreeNode other = (BookmarkTreeNode) obj;
        return bookmark.getId().equals(other.bookmark.getId());
    }

    @Override
    public int hashCode() {
        return bookmark.getId().hashCode();
    }

    @Override
    public String toString() {
        return "BookmarkTreeNode[" + bookmark.toString() + "]";
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isInstance(bookmark)) {
            return adapter.cast(bookmark);
        }
        return null;
    }
}

