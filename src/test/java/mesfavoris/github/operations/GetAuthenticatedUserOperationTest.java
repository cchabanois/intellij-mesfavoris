package mesfavoris.github.operations;

import mesfavoris.remote.UserInfo;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GetAuthenticatedUserOperationTest extends AbstractGithubOperationTest {

    @Test
    public void testGetAuthenticatedUser_returnsUserInfo() throws Exception {
        GetAuthenticatedUserOperation op = new GetAuthenticatedUserOperation(apiClient);

        UserInfo userInfo = op.getAuthenticatedUser(null);

        assertThat(userInfo.getEmailAddress()).isNotNull();
    }
}
