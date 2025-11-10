package mesfavoris.gdrive.test;

import mesfavoris.gdrive.connection.IGDriveUserInfoStore;
import mesfavoris.remote.UserInfo;
import org.jetbrains.annotations.Nullable;

/**
 * Simple in-memory implementation of IGDriveUserInfoStore for tests
 */
public class InMemoryGDriveUserInfoStore implements IGDriveUserInfoStore {

    private UserInfo userInfo;

    @Override
    @Nullable
    public UserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public void setUserInfo(@Nullable UserInfo userInfo) {
        this.userInfo = userInfo;
    }
}

