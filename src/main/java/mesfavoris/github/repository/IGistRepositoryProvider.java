package mesfavoris.github.repository;

/** Gives access to the local clone of a gist. */
public interface IGistRepositoryProvider {

    GistRepository getGistRepository(String gistId, String gitUrl);
}
