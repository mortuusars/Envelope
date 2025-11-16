package io.github.mortuusars.envelope.util.validation;

import java.util.ArrayList;
import java.util.List;

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

    public List<Issue> validate(T value) {
        List<Issue> issues = new ArrayList<>();

        for (Rule<T> rule : rules) {
            rule.test(value).ifPresent(issues::add);
        }

        return issues;
    }

    public CachedValidator<T> cached() {
        return new CachedValidator<>(this.rules);
    }
}
