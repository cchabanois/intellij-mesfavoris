package mesfavoris.commons;

import java.util.Optional;

public interface IAdaptable {

    public <T> T getAdapter(Class<T> adapter);

}
