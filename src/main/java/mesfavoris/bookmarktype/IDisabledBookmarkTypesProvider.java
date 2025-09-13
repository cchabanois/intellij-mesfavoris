package mesfavoris.bookmarktype;

import java.util.Set;

public interface IDisabledBookmarkTypesProvider {
    default boolean isBookmarkTypeEnabled(String bookmarkTypeName) {
        return !getDisabledBookmarkTypes().contains(bookmarkTypeName);
    }

    Set<String> getDisabledBookmarkTypes();
}
