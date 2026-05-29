package mesfavoris.github.connection;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.Attribute;
import mesfavoris.remote.UserInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persists the authenticated GitHub user's login and display name to {@code mesfavoris.xml}.
 * Allows showing "Connected as X" across IDE restarts without an extra API call.
 */
@Service(Service.Level.PROJECT)
@State(name = "GithubUserInfo", storages = @Storage("mesfavoris.xml"))
public final class GithubUserInfoStore implements PersistentStateComponent<GithubUserInfoStore.State> {

    private State state = new State();

    @Nullable
    public UserInfo getUserInfo() {
        if (state.login == null) {
            return null;
        }
        return new UserInfo(state.login, state.displayName);
    }

    public void setUserInfo(@Nullable UserInfo userInfo) {
        if (userInfo == null) {
            state.login = null;
            state.displayName = null;
        } else {
            state.login = userInfo.getEmailAddress();
            state.displayName = userInfo.getDisplayName();
        }
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public static class State {
        @Attribute
        public String login;

        @Attribute
        public String displayName;
    }
}
