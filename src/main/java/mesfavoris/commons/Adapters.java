package mesfavoris.commons;

public class Adapters {

    public static <T> T adapt(Object sourceObject, Class<T> adapter) {
        if (sourceObject == null) {
            return null;
        }
        if (adapter.isInstance(sourceObject)) {
            return (T) sourceObject;
        }

        if (sourceObject instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) sourceObject;

            Object result = adaptable.getAdapter(adapter);
            if (result != null) {
                // Sanity-check
                if (!adapter.isInstance(result)) {
                    return null;
                }
                return (T) result;
            }
        }
        return null;
    }

}
