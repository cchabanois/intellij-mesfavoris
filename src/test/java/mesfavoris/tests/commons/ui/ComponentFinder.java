package mesfavoris.tests.commons.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Predicate;

/**
 * Utility class to find components in a Swing component hierarchy
 */
public class ComponentFinder {

    /**
     * Find a child component of the specified type in the component hierarchy
     *
     * @param parent the parent component to search in
     * @param componentClass the class or interface of the component to find
     * @param <T> the type of component to find
     * @return the first component found, or null if not found
     */
    public static <T> T findChildComponent(JComponent parent, Class<T> componentClass) {
        return findChildComponent(parent, componentClass, null);
    }

    /**
     * Find a child component of the specified type in the component hierarchy that matches the predicate
     *
     * @param parent the parent component to search in
     * @param componentClass the class or interface of the component to find
     * @param predicate optional predicate to filter components (can be null)
     * @param <T> the type of component to find
     * @return the first component found that matches the predicate, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T findChildComponent(JComponent parent, Class<T> componentClass, Predicate<T> predicate) {
        if (componentClass.isInstance(parent)) {
            T component = (T) parent;
            if (predicate == null || predicate.test(component)) {
                return component;
            }
        }

        for (Component child : parent.getComponents()) {
            if (child instanceof JComponent) {
                T found = findChildComponent((JComponent) child, componentClass, predicate);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}

