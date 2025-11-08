package mesfavoris.gdrive.connection.store;

import com.google.api.client.util.store.AbstractDataStoreFactory;
import com.google.api.client.util.store.DataStore;
import com.intellij.ide.passwordSafe.PasswordSafe;

import java.io.Serializable;

/**
 * DataStoreFactory implementation that creates PasswordSafeDataStore instances.
 * This factory creates data stores that use IntelliJ's PasswordSafe for secure credential storage.
 */
public class PasswordSafeDataStoreFactory extends AbstractDataStoreFactory {

    private final PasswordSafe passwordSafe;

    public PasswordSafeDataStoreFactory(PasswordSafe passwordSafe) {
        this.passwordSafe = passwordSafe;
    }

    public PasswordSafeDataStoreFactory() {
        this(PasswordSafe.getInstance());
    }

    @Override
    protected <V extends Serializable> DataStore<V> createDataStore(String id) {
        @SuppressWarnings("unchecked")
        DataStore<V> dataStore = (DataStore<V>) new PasswordSafeDataStore(this, id, passwordSafe);
        return dataStore;
    }
}

