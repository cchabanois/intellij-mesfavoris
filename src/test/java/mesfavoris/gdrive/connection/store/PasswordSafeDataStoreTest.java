package mesfavoris.gdrive.connection.store;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class PasswordSafeDataStoreTest extends BasePlatformTestCase {

    private PasswordSafeDataStoreFactory factory;
    private DataStore<StoredCredential> dataStore;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        factory = new PasswordSafeDataStoreFactory(PasswordSafe.getInstance());
        dataStore = factory.getDataStore("teststore");
        // Clean up any existing data
        dataStore.clear();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            // Clean up after tests
            if (dataStore != null) {
                dataStore.clear();
            }
        } finally {
            super.tearDown();
        }
    }

    public void testGetReturnsNullWhenCredentialNotFound() throws IOException {
        // When
        StoredCredential credential = dataStore.get("nonexistent");

        // Then
        assertThat(credential).isNull();
    }

    
    public void testSetAndGetCredential() throws IOException {
        // Given
        StoredCredential credential = new StoredCredential();
        credential.setAccessToken("access-token-123");
        credential.setRefreshToken("refresh-token-456");
        credential.setExpirationTimeMilliseconds(1234567890L);

        // When
        dataStore.set("user", credential);

        // Then
        StoredCredential retrieved = dataStore.get("user");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getAccessToken()).isEqualTo("access-token-123");
        assertThat(retrieved.getRefreshToken()).isEqualTo("refresh-token-456");
        assertThat(retrieved.getExpirationTimeMilliseconds()).isEqualTo(1234567890L);
    }

    
    public void testDeleteCredential() throws IOException {
        // Given
        StoredCredential credential = new StoredCredential();
        credential.setAccessToken("access-token");
        dataStore.set("user", credential);

        // When
        dataStore.delete("user");

        // Then
        assertThat(dataStore.get("user")).isNull();
        assertThat(dataStore.containsKey("user")).isFalse();
    }

    
    public void testClearRemovesAllCredentials() throws IOException {
        // Given
        StoredCredential credential1 = new StoredCredential();
        credential1.setAccessToken("token1");
        StoredCredential credential2 = new StoredCredential();
        credential2.setAccessToken("token2");

        dataStore.set("user1", credential1);
        dataStore.set("user2", credential2);

        // When
        dataStore.clear();

        // Then
        assertThat(dataStore.isEmpty()).isTrue();
        assertThat(dataStore.size()).isEqualTo(0);
    }

    
    public void testKeySetReturnsAllKeys() throws IOException {
        // Given
        StoredCredential credential1 = new StoredCredential();
        credential1.setAccessToken("token1");
        StoredCredential credential2 = new StoredCredential();
        credential2.setAccessToken("token2");

        dataStore.set("user1", credential1);
        dataStore.set("user2", credential2);

        // When
        Set<String> keys = dataStore.keySet();

        // Then
        assertThat(keys).containsExactlyInAnyOrder("user1", "user2");
    }

    
    public void testContainsKey() throws IOException {
        // Given
        StoredCredential credential = new StoredCredential();
        credential.setAccessToken("token");
        dataStore.set("user", credential);

        // When/Then
        assertThat(dataStore.containsKey("user")).isTrue();
        assertThat(dataStore.containsKey("nonexistent")).isFalse();
    }

    
    public void testSize() throws IOException {
        // Given
        assertThat(dataStore.size()).isEqualTo(0);

        StoredCredential credential1 = new StoredCredential();
        credential1.setAccessToken("token1");
        StoredCredential credential2 = new StoredCredential();
        credential2.setAccessToken("token2");

        // When
        dataStore.set("user1", credential1);
        dataStore.set("user2", credential2);

        // Then
        assertThat(dataStore.size()).isEqualTo(2);
    }

    
    public void testIsEmpty() throws IOException {
        // Given
        assertThat(dataStore.isEmpty()).isTrue();

        StoredCredential credential = new StoredCredential();
        credential.setAccessToken("token");

        // When
        dataStore.set("user", credential);

        // Then
        assertThat(dataStore.isEmpty()).isFalse();
    }

    
    public void testValues() throws IOException {
        // Given
        StoredCredential credential1 = new StoredCredential();
        credential1.setAccessToken("token1");
        credential1.setRefreshToken("refresh1");

        StoredCredential credential2 = new StoredCredential();
        credential2.setAccessToken("token2");
        credential2.setRefreshToken("refresh2");

        dataStore.set("user1", credential1);
        dataStore.set("user2", credential2);

        // When
        Collection<StoredCredential> values = dataStore.values();

        // Then
        assertThat(values).hasSize(2);
        assertThat(values).extracting(StoredCredential::getAccessToken)
                .containsExactlyInAnyOrder("token1", "token2");
    }

    
    public void testSetNullValueDeletesCredential() throws IOException {
        // Given
        StoredCredential credential = new StoredCredential();
        credential.setAccessToken("token");
        dataStore.set("user", credential);

        // When
        dataStore.set("user", null);

        // Then
        assertThat(dataStore.containsKey("user")).isFalse();
    }

    
    public void testGetDataStoreFactory() {
        // When/Then
        assertThat(dataStore.getDataStoreFactory()).isEqualTo(factory);
    }

    
    public void testGetId() {
        // When/Then
        assertThat(dataStore.getId()).isEqualTo("teststore");
    }
}

