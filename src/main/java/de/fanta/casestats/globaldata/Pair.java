package de.fanta.casestats.globaldata;

import java.util.Map;
import java.util.Objects;

public class Pair<T, S> {

    public final T first;
    public final S second;

    public Pair(T first, S second) {
        this.first = first;
        this.second = second;
    }

    @SuppressWarnings("unchecked")
    public Pair(Map<String, Object> serialized) {
        this.first = (T) serialized.get("first");
        this.second = (S) serialized.get("second");
    }

    public <X> Pair<X, S> setFirst(X first) {
        return new Pair<>(first, this.second);
    }

    public <X> Pair<T, X> setSecond(X second) {
        return new Pair<>(this.first, second);
    }

    public T first() {
        return this.first;
    }

    public S second() {
        return this.second;
    }

    @Override
    public String toString() {
        return "(" + this.first + ";" + this.second + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.first) + 31 * Objects.hashCode(this.second);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Pair<?, ?>)) {
            return false;
        }
        Pair<?, ?> op = (Pair<?, ?>) other;
        return Objects.equals(this.first, op.first) && Objects.equals(this.second, op.second);
    }

}
