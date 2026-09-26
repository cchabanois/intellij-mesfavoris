package mesfavoris.github.mappings;
import mesfavoris.github.client.GistResponse;

import mesfavoris.model.BookmarksTree;

import java.util.HashMap;
import java.util.Map;

/** Extracts display properties (URL, owner login, bookmark count) and the git URL from a Gist API response. */
public class GistMappingPropertiesProvider {

    public Map<String, String> getProperties(GistResponse response, BookmarksTree bookmarksTree) {
        Map<String, String> props = new HashMap<>();
        if (response.html_url != null) {
            props.put(GistMapping.PROP_GIST_URL, response.html_url);
        }
        if (response.owner != null && response.owner.login != null) {
            props.put(GistMapping.PROP_OWNER_LOGIN, response.owner.login);
        }
        if (response.git_pull_url != null) {
            props.put(GistMapping.PROP_GIT_URL, response.git_pull_url);
        }
        return withBookmarksCount(props, bookmarksTree);
    }

    /** {@code properties} with the bookmark count updated for {@code bookmarksTree}. */
    public Map<String, String> withBookmarksCount(Map<String, String> properties, BookmarksTree bookmarksTree) {
        Map<String, String> props = new HashMap<>(properties);
        props.put(GistMapping.PROP_BOOKMARKS_COUNT, Integer.toString(bookmarksTree.size() - 1));
        return props;
    }
}
