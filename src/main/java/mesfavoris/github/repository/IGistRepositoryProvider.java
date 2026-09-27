package mesfavoris.github.repository;

import java.io.IOException;

/** Gives access to the local clone of a gist. */
public interface IGistRepositoryProvider {

    GistRepository getGistRepository(String gistId) throws IOException;
}
