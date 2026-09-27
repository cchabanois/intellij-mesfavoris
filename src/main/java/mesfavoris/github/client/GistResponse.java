package mesfavoris.github.client;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/** A GitHub Gist as returned by the REST API. */
public class GistResponse {
    public String id;
    public String description;
    public String updated_at;
    public String html_url;
    public String git_pull_url;
    public GistOwner owner;
    public Map<String, GistFile> files;
    /** Revisions, latest first; {@code version} is the commit id in the gist's git repository. */
    public List<GistRevision> history;

    public static class GistOwner {
        public String login;
    }

    public static class GistRevision {
        public String version;
    }

    /** The commit id of the latest revision, or null if the response has no history. */
    @Nullable
    public String latestVersion() {
        return history != null && !history.isEmpty() ? history.getFirst().version : null;
    }
}
