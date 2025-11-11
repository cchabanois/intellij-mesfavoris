package mesfavoris.gdrive.connection;

import mesfavoris.remote.UserInfo;
import org.jetbrains.annotations.Nullable;

public interface IGDriveUserInfoStore {

    UserInfo getUserInfo();

    void setUserInfo(@Nullable UserInfo userInfo);

}
