package de.fanta.casestats.globaldata;

import java.util.function.Function;
import java.util.function.Predicate;

public class FunctionUtil {
    private FunctionUtil() {
        throw new UnsupportedOperationException("No instance for you, Sir!");
        // prevents instances
    }

    public static <T> Predicate<T> negate(Predicate<T> predicate) {
        return predicate.negate();
    }

    public static <S, T> Predicate<S> functionPredicate(Predicate<T> predicate, Function<S, T> function) {
        return s -> predicate.test(function.apply(s));
    }
}
