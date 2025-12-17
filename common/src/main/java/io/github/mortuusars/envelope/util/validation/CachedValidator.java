package io.github.mortuusars.envelope.util.validation;

import io.github.mortuusars.envelope.util.result.Error;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CachedValidator<T> extends Validator<T> {
    protected @Nullable T value = null;
    protected List<Error> issues = Collections.emptyList();

    public CachedValidator(List<Rule<T>> rules) {
        super(rules);
    }

    public @Nullable T getValue() {
        return value;
    }

    public List<Error> getErrors() {
        return issues;
    }

    @Override
    public List<Error> testAll(T value) {
        List<Error> errors = super.testAll(value);
        this.value = value;
        this.issues = errors;
        return errors;
    }
}
