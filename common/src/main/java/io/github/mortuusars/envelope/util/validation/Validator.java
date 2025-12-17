package io.github.mortuusars.envelope.util.validation;

import io.github.mortuusars.envelope.util.result.Error;
import io.github.mortuusars.envelope.util.result.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Validator<T> {
    List<Rule<T>> rules;

    public Validator(List<Rule<T>> rules) {
        this.rules = rules;
    }

    @SafeVarargs
    public static <T> Validator<T> of(Rule<T>... rules) {
        return new Validator<>(List.of(rules));
    }

    public Validator<T> and(Rule<T> rule) {
        List<Rule<T>> rules = new ArrayList<>(this.rules);
        rules.add(rule);
        return new Validator<>(rules);
    }

    public Validator<T> and(Validator<T> another) {
        List<Rule<T>> rules = new ArrayList<>(this.rules);
        rules.addAll(another.rules);
        return new Validator<>(rules);
    }

    // --

    public List<Error> testAll(T value) {
        List<Error> errors = new ArrayList<>();

        for (Rule<T> rule : rules) {
            rule.test(value).ifPresent(errors::add);
        }

        return errors;
    }

    public Result<T> test(T value) {
        for (Rule<T> rule : rules) {
            Optional<Error> test = rule.test(value);
            if (test.isPresent()) {
                return Result.error(test.get());
            }
        }
        return Result.success(value);
    }

    public CachedValidator<T> cached() {
        return new CachedValidator<>(this.rules);
    }
}
