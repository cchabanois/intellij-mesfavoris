package mesfavoris.gdrive.connection;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.Attribute;
import mesfavoris.remote.UserInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.PROJECT)
@State(name = "GDriveUserInfo", storages = @Storage("mesfavoris-gdrive.xml"))
public final class GDriveUserInfoStore implements PersistentStateComponent<GDriveUserInfoStore.State>, IGDriveUserInfoStore {

    private State state = new State();

    @Override
    @Nullable
    public UserInfo getUserInfo() {
        if (state.email == null) {
            return null;
        }
        return new UserInfo(state.email, state.displayName);
    }

    @Override
    public void setUserInfo(@Nullable UserInfo userInfo) {
        if (userInfo == null) {
            state.email = null;
            state.displayName = null;
        } else {
            state.email = userInfo.getEmailAddress();
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
        public String email;

        @Attribute
        public String displayName;
    }
}

