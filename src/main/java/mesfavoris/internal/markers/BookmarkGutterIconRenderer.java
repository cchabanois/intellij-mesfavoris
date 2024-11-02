package mesfavoris.internal.markers;

import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Comparing;
import mesfavoris.bookmarktype.BookmarkMarker;
import mesfavoris.icons.MesFavorisIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;

public class BookmarkGutterIconRenderer extends GutterIconRenderer implements DumbAware {
    private final BookmarkMarker bookmarkMarker;

    public BookmarkGutterIconRenderer(BookmarkMarker bookmarkMarker) {
        this.bookmarkMarker = bookmarkMarker;
    }

    @Override
    public @NotNull Icon getIcon() {
        return MesFavorisIcons.bookmark;
    }

    public BookmarkMarker getBookmarkMarker() {
        return bookmarkMarker;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookmarkGutterIconRenderer that = (BookmarkGutterIconRenderer) o;
        return bookmarkMarker.equals(that.bookmarkMarker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookmarkMarker);
    }
}
