package mesfavoris.github.repository;

import com.intellij.openapi.util.ThrowableComputable;

import java.io.IOException;

/** Gives access to the local clone of a gist. */
public interface IGistRepositoryProvider {

    /** @param gitUrlSupplier gives the gist's git url, only asked for when the clone has to be created */
    GistRepository getGistRepository(String gistId, ThrowableComputable<String, IOException> gitUrlSupplier)
            throws IOException;
}
