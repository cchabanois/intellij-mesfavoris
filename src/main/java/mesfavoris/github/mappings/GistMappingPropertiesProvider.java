package mesfavoris.github.mappings;

import mesfavoris.github.operations.GistApiClient;
import mesfavoris.model.BookmarksTree;

import java.util.HashMap;
import java.util.Map;

/** Extracts display properties (URL, owner login, bookmark count) from a Gist API response. */
public class GistMappingPropertiesProvider {

    public Map<String, String> getProperties(GistApiClient.GistResponse response, BookmarksTree bookmarksTree) {
        Map<String, String> props = new HashMap<>();
        if (response.html_url != null) {
            props.put(GistMapping.PROP_GIST_URL, response.html_url);
        }
        if (response.owner != null && response.owner.login != null) {
            props.put(GistMapping.PROP_OWNER_LOGIN, response.owner.login);
        }
        props.put(GistMapping.PROP_BOOKMARKS_COUNT, Integer.toString(bookmarksTree.size() - 1));
        return props;
    }
}
