package mesfavoris.internal.ui.virtual;

import mesfavoris.commons.IAdaptable;
import mesfavoris.model.Bookmark;
import mesfavoris.model.BookmarkId;

/**
 * Mutable wrapper for immutable BookmarkLink objects used in the tree model.
 * This allows the tree to maintain stable TreePath references while the underlying
 * Bookmark instances are replaced when modified.
 */
public class BookmarkLinkNode implements IAdaptable {
    private BookmarkLink bookmarkLink;

    public BookmarkLinkNode(BookmarkLink bookmarkLink) {
        if (bookmarkLink == null) {
            throw new IllegalArgumentException("BookmarkLink cannot be null");
        }
        this.bookmarkLink = bookmarkLink;
    }

    /**
     * Update the wrapped bookmark link instance.
     * This is called when the bookmark is modified in the database.
     */
    public void updateBookmarkLink(BookmarkLink newLink) {
        if (newLink == null) {
            throw new IllegalArgumentException("BookmarkLink cannot be null");
        }
        if (!newLink.getBookmark().getId().equals(this.bookmarkLink.getBookmark().getId())) {
            throw new IllegalArgumentException("Cannot change bookmark ID");
        }
        this.bookmarkLink = newLink;
    }

    public BookmarkLink getBookmarkLink() {
        return bookmarkLink;
    }

    public BookmarkId getBookmarkId() {
        return bookmarkLink.getBookmark().getId();
    }

    public Bookmark getBookmark() {
        return bookmarkLink.getBookmark();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BookmarkLinkNode other = (BookmarkLinkNode) obj;
        return bookmarkLink.getBookmark().getId().equals(other.bookmarkLink.getBookmark().getId());
    }

    @Override
    public int hashCode() {
        return bookmarkLink.getBookmark().getId().hashCode();
    }

    @Override
    public String toString() {
        return "BookmarkLinkNode[" + bookmarkLink.getBookmark().toString() + "]";
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == BookmarkLink.class) {
            return adapter.cast(bookmarkLink);
        }
        if (adapter == Bookmark.class) {
            return adapter.cast(bookmarkLink.getBookmark());
        }
        return null;
    }
}

