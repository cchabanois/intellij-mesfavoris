package mesfavoris.github.integration;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Resolves the GitHub account (token + API base URL + login) to use for a given project.
 */
public interface IGithubAccountResolver {

    /**
     * Resolves the GitHub account to use for the given project.
     * If multiple accounts are configured and none is set as default,
     * prompts the user to choose one.
     *
     * @return account info, or null if no account is available or the user cancelled
     */
    @Nullable
    GithubAccountInfo resolveAccount(@NotNull Project project) throws IOException;

    record GithubAccountInfo(String accessToken, String apiBaseUrl, String login) {
    }
}
