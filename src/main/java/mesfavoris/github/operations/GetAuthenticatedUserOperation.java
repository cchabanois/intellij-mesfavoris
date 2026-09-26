package mesfavoris.github.operations;
import mesfavoris.github.client.UserResponse;
import mesfavoris.github.client.IGistApiClient;

import com.intellij.openapi.progress.ProgressIndicator;
import mesfavoris.remote.UserInfo;

import java.io.IOException;

/** Fetches the authenticated user's login and name; used to verify the token on connect. */
public class GetAuthenticatedUserOperation {
    private final IGistApiClient apiClient;

    public GetAuthenticatedUserOperation(IGistApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public UserInfo getAuthenticatedUser(ProgressIndicator indicator) throws IOException {
        if (indicator != null) {
            indicator.setText("Getting authenticated GitHub user info");
        }
        UserResponse response = apiClient.getAuthenticatedUser();
        String displayName = response.name != null ? response.name : response.login;
        return new UserInfo(response.login, displayName);
    }
}
