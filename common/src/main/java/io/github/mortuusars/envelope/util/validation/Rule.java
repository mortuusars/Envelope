package io.github.mortuusars.envelope.util.validation;

import java.util.Optional;
import java.util.function.Predicate;

public interface Rule<T> {
    Optional<Issue> test(T value);

    static <T> Rule<T> when(Predicate<T> predicate, Issue issue) {
        return new SimpleRule<>(predicate, issue);
    }

    record SimpleRule<T>(Predicate<T> predicate, Issue issue) implements Rule<T> {
        public Optional<Issue> test(T value) {
            return predicate.test(value) ? Optional.of(issue) : Optional.empty();
        }
    }
}
