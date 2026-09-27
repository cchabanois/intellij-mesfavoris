package mesfavoris.internal.persistence;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import mesfavoris.model.BookmarksTree;
import mesfavoris.persistence.IBookmarksTreeSerializer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public class LocalBookmarksSaver {
    private static final Logger LOG = Logger.getInstance(LocalBookmarksSaver.class);
    private final File file;
    private final IBookmarksTreeSerializer bookmarksSerializer;

    public LocalBookmarksSaver(@NotNull File file, @NotNull IBookmarksTreeSerializer bookmarksSerializer) {
        this.file = file;
        this.bookmarksSerializer = bookmarksSerializer;
    }

    public void saveBookmarks(BookmarksTree bookmarksTree) {
        try {
            Path target = file.toPath().toAbsolutePath();
            Files.createDirectories(target.getParent());
            // write to a temp file then move it, so a crash never leaves a truncated bookmarks file
            // not Files.createTempFile: it would make the bookmarks file owner-only readable
            Path temp = target.resolveSibling(target.getFileName() + ".tmp");
            try {
                try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                     Writer writer = Channels.newWriter(channel, StandardCharsets.UTF_8)) {
                    bookmarksSerializer.serialize(bookmarksTree, bookmarksTree.getRootFolder().getId(), writer);
                    writer.flush();
                    channel.force(true);
                }
                replace(temp, target);
            } finally {
                Files.deleteIfExists(temp);
            }
            // Reload the file in any open editors
            refreshFile();
        } catch (IOException e) {
            LOG.error("Failed to save bookmarks", e);
        }
    }

    private static void replace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
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
