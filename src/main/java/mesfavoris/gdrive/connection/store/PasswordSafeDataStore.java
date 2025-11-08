package mesfavoris.gdrive.connection.store;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.gson.Gson;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

import java.io.IOException;
import java.util.*;

/**
 * DataStore implementation that uses IntelliJ's PasswordSafe to securely store credentials.
 * This is specifically designed for storing Google OAuth StoredCredential objects.
 */
public class PasswordSafeDataStore implements DataStore<StoredCredential> {

    private static final String KEYS_KEY = "__keys__";

    private final DataStoreFactory dataStoreFactory;
    private final String id;
    private final PasswordSafe passwordSafe;
    private final Gson gson = new Gson();

    // Cache to track which keys exist (PasswordSafe doesn't provide a way to list all keys)
    private final Set<String> keyCache = new HashSet<>();

    public PasswordSafeDataStore(DataStoreFactory dataStoreFactory, String id, PasswordSafe passwordSafe) {
        this.dataStoreFactory = dataStoreFactory;
        this.id = id;
        this.passwordSafe = passwordSafe;
        loadKeyCache();
    }

    @Override
    public DataStoreFactory getDataStoreFactory() {
        return dataStoreFactory;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int size() throws IOException {
        return keyCache.size();
    }

    @Override
    public boolean isEmpty() throws IOException {
        return keyCache.isEmpty();
    }

    @Override
    public boolean containsKey(String key) {
        return keyCache.contains(key);
    }

    @Override
    public boolean containsValue(StoredCredential value) throws IOException {
        if (value == null) {
            return false;
        }
        for (String key : keyCache) {
            StoredCredential storedValue = get(key);
            if (value.equals(storedValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> keySet() {
        return new HashSet<>(keyCache);
    }

    @Override
    public Collection<StoredCredential> values() throws IOException {
        List<StoredCredential> values = new ArrayList<>();
        for (String key : keyCache) {
            StoredCredential value = get(key);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    @Override
    public StoredCredential get(String key) throws IOException {
        CredentialAttributes attributes = createCredentialAttributes(key);
        String password = passwordSafe.getPassword(attributes);

        if (password == null) {
            return null;
        }

        // Deserialize from JSON
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = gson.fromJson(password, Map.class);

            StoredCredential credential = new StoredCredential();
            credential.setAccessToken((String) map.get("accessToken"));
            credential.setRefreshToken((String) map.get("refreshToken"));

            Object expirationTime = map.get("expirationTimeMilliseconds");
            if (expirationTime instanceof Number) {
                credential.setExpirationTimeMilliseconds(((Number) expirationTime).longValue());
            }

            return credential;
        } catch (Exception e) {
            throw new IOException("Failed to deserialize credential for key: " + key, e);
        }
    }

    @Override
    public DataStore<StoredCredential> set(String key, StoredCredential value) throws IOException {
        if (value == null) {
            return delete(key);
        }

        CredentialAttributes attributes = createCredentialAttributes(key);

        // Serialize to JSON using a Map
        Map<String, Object> map = new HashMap<>();
        map.put("accessToken", value.getAccessToken());
        map.put("refreshToken", value.getRefreshToken());
        map.put("expirationTimeMilliseconds", value.getExpirationTimeMilliseconds());

        String json = gson.toJson(map);

        // Store in PasswordSafe (username is the key, password is the JSON)
        Credentials credentials = new Credentials(key, json);
        passwordSafe.set(attributes, credentials);

        // Update cache
        keyCache.add(key);
        saveKeyCache();

        return this;
    }

    @Override
    public DataStore<StoredCredential> clear() throws IOException {
        for (String key : new HashSet<>(keyCache)) {
            delete(key);
        }
        return this;
    }

    @Override
    public DataStore<StoredCredential> delete(String key) throws IOException {
        CredentialAttributes attributes = createCredentialAttributes(key);
        passwordSafe.set(attributes, null);

        // Update cache
        keyCache.remove(key);
        saveKeyCache();

        return this;
    }

    private CredentialAttributes createCredentialAttributes(String key) {
        return new CredentialAttributes(
            CredentialAttributesKt.generateServiceName("GoogleDrive", id + ":" + key)
        );
    }

    private void loadKeyCache() {
        // Load the list of keys from a special entry in PasswordSafe
        CredentialAttributes keyCacheAttributes = createCredentialAttributes(KEYS_KEY);
        String keysJson = passwordSafe.getPassword(keyCacheAttributes);

        if (keysJson != null && !keysJson.isEmpty()) {
            try {
                String[] keys = gson.fromJson(keysJson, String[].class);
                keyCache.addAll(Arrays.asList(keys));
            } catch (Exception e) {
                // If deserialization fails, start with empty cache
                keyCache.clear();
            }
        }
    }

    private void saveKeyCache() {
        // Save the list of keys to a special entry in PasswordSafe
        CredentialAttributes keyCacheAttributes = createCredentialAttributes(KEYS_KEY);

        String[] keys = keyCache.toArray(new String[0]);
        String keysJson = gson.toJson(keys);

        Credentials credentials = new Credentials(KEYS_KEY, keysJson);
        passwordSafe.set(keyCacheAttributes, credentials);
    }
}
