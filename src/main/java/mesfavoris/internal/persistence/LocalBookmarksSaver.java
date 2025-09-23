package mesfavoris.internal.persistence;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksTreeSerializer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LocalBookmarksSaver {
    private static final Logger LOG = Logger.getInstance(LocalBookmarksSaver.class);
    private final File file;
    private final IBookmarksTreeSerializer bookmarksSerializer;

    public LocalBookmarksSaver(@NotNull File file, @NotNull IBookmarksTreeSerializer bookmarksSerializer) {
        this.file = file;
        this.bookmarksSerializer = bookmarksSerializer;
    }

    public void saveBookmarks(BookmarksTree bookmarksTree) {
        try (FileWriter writer = new FileWriter(file)) {
            bookmarksSerializer.serialize(bookmarksTree,
                    bookmarksTree.getRootFolder().getId(), writer);

            // Reload the file in any open editors
            refreshFile();
        } catch (IOException e) {
            LOG.error("Failed to save bookmarks", e);
        }
    }

    private void refreshFile() {
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
        if (virtualFile != null) {
            // Refresh VFS to detect file changes
            virtualFile.refresh(false, false);
        }
    }

}
