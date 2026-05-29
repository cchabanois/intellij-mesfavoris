package mesfavoris.github.operations;

import mesfavoris.remote.UserInfo;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class GetAuthenticatedUserOperationTest {
    private GistApiClient mockApiClient;
    private GetAuthenticatedUserOperation operation;

    @Before
    public void setUp() {
        mockApiClient = mock(GistApiClient.class);
        operation = new GetAuthenticatedUserOperation(mockApiClient);
    }

    @Test
    public void testGetAuthenticatedUser_returnsUserInfo() throws Exception {
        GistApiClient.UserResponse userResponse = new GistApiClient.UserResponse();
        userResponse.login = "cchabanois";
        userResponse.name = "Cedric Chabanois";
        when(mockApiClient.getAuthenticatedUser()).thenReturn(userResponse);

        UserInfo userInfo = operation.getAuthenticatedUser(null);

        assertThat(userInfo.getEmailAddress()).isEqualTo("cchabanois");
        assertThat(userInfo.getDisplayName()).isEqualTo("Cedric Chabanois");
    }

    @Test
    public void testGetAuthenticatedUser_usesLoginWhenNameIsNull() throws Exception {
        GistApiClient.UserResponse userResponse = new GistApiClient.UserResponse();
        userResponse.login = "cchabanois";
        userResponse.name = null;
        when(mockApiClient.getAuthenticatedUser()).thenReturn(userResponse);

        UserInfo userInfo = operation.getAuthenticatedUser(null);

        assertThat(userInfo.getEmailAddress()).isEqualTo("cchabanois");
        assertThat(userInfo.getDisplayName()).isEqualTo("cchabanois");
    }
}
