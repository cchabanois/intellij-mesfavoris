package mesfavoris.commons;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A generic pair of two values.
 * 
 * @param <U> The type of the first value
 * @param <V> The type of the second value
 */
public class Pair<U, V> {
    private final U first;
    private final V second;

    public Pair(@Nullable U first, @Nullable V second) {
        this.first = first;
        this.second = second;
    }

    @Nullable
    public U getFirst() {
        return first;
    }

    @Nullable
    public V getSecond() {
        return second;
    }

    /**
     * Create a new pair with the given values
     */
    @NotNull
    public static <U, V> Pair<U, V> of(@Nullable U first, @Nullable V second) {
        return new Pair<>(first, second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(first, pair.first) &&
               Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "Pair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
