package mesfavoris.extensions;

import org.jetbrains.annotations.NotNull;

/**
 * Wrapper class that associates an element with its priority.
 * This allows each element (provider, handler, part, etc.) to have its own priority
 * without modifying the existing interfaces.
 *
 * @param <T> The type of element being wrapped
 */
public class PrioritizedElement<T> implements Comparable<PrioritizedElement<T>> {

    private final T element;
    private final int priority;

    public PrioritizedElement(@NotNull T element, int priority) {
        this.element = element;
        this.priority = priority;
    }

    public PrioritizedElement(@NotNull T element) {
        this(element, 10); // Default priority
    }

    @NotNull
    public T getElement() {
        return element;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(@NotNull PrioritizedElement<T> other) {
        // Lower priority value first (ascending order)
        return Integer.compare(this.priority, other.priority);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        PrioritizedElement<?> that = (PrioritizedElement<?>) obj;
        return priority == that.priority && element.equals(that.element);
    }

    @Override
    public int hashCode() {
        return element.hashCode() * 31 + priority;
    }

    @Override
    public String toString() {
        return "PrioritizedElement{" +
                "element=" + element.getClass().getSimpleName() +
                ", priority=" + priority +
                '}';
    }
}
