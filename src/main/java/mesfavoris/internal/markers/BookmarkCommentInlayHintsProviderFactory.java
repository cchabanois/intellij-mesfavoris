package mesfavoris.internal.markers;

import com.intellij.codeInsight.hints.InlayHintsProvider;
import com.intellij.codeInsight.hints.InlayHintsProviderFactory;
import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Factory that provides BookmarkCommentInlayHintsProvider for all languages.
 * This allows registering the provider once instead of for each language separately.
 */
@SuppressWarnings("UnstableApiUsage")
public class BookmarkCommentInlayHintsProviderFactory implements InlayHintsProviderFactory {

    @Override
    public @NotNull List<InlayHintsProvider<?>> getProvidersInfoForLanguage(@NotNull Language language) {
        return List.of(new BookmarkCommentInlayHintsProvider());
    }
}

