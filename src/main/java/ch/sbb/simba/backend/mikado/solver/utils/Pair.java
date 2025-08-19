package ch.sbb.simba.backend.mikado.solver.utils;

import java.util.Objects;
import lombok.Getter;

@Getter
public class Pair<F, S> {

    private final F first;
    private final S second;

    // Constructor
    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public static <F, S> Pair<F, S> of(F first, S second) {
        return new Pair<>(first, second);
    }

    // Override equals() to compare pairs
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Check if the same instance
        if (!(o instanceof Pair<?, ?> pair)) return false; // Check if the object is a Pair
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second); // Compare first and second by object
    }

    // Override hashCode() to maintain the contract with equals()
    @Override
    public int hashCode() {
        return Objects.hash(first, second); // Generate hash code based on first and second
    }

}
