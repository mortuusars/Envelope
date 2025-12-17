package io.github.mortuusars.envelope.util.validation;

import io.github.mortuusars.envelope.util.result.Error;

import java.util.Optional;
import java.util.function.Predicate;

public interface Rule<T> {
    Optional<Error> test(T value);

    static <T> Rule<T> when(Predicate<T> predicate, Error issue) {
        return new SimpleRule<>(predicate, issue);
    }

    record SimpleRule<T>(Predicate<T> predicate, Error issue) implements Rule<T> {
        public Optional<Error> test(T value) {
            return predicate.test(value) ? Optional.of(issue) : Optional.empty();
        }
    }
}
