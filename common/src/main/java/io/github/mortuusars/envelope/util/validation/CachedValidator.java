package io.github.mortuusars.envelope.util.validation;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CachedValidator<T> extends Validator<T> {
    protected @Nullable T value = null;
    protected List<Issue> issues = Collections.emptyList();

    public CachedValidator(List<Rule<T>> rules) {
        super(rules);
    }

    public @Nullable T getValue() {
        return value;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    @Override
    public List<Issue> validate(T value) {
        List<Issue> issues = super.validate(value);
        this.value = value;
        this.issues = issues;
        return issues;
    }
}
