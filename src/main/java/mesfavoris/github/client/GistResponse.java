package mesfavoris.github.client;

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

    public static class GistOwner {
        public String login;
    }
}
