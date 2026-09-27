package mesfavoris.github.repository;

import com.intellij.openapi.util.io.NioFiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/** A gist's clone on disk: its directory, and the lock every git command on it must hold. */
final class GistClone {
    private final Path directory;
    private final Lock lock = new ReentrantLock();

    GistClone(Path directory) {
        this.directory = directory;
    }

    Path getDirectory() {
        return directory;
    }

    Lock getLock() {
        return lock;
    }

    void delete() throws IOException {
        lock.lock();
        try {
            NioFiles.deleteRecursively(directory);
        } finally {
            lock.unlock();
        }
    }
}
