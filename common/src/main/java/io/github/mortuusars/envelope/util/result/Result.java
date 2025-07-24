package io.github.mortuusars.envelope.util.result;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class Result<T> {
    @Nullable
    protected final T value;
    @Nullable
    protected final Failure failure;

    protected Result(@Nullable T value, @Nullable Failure failure) {
        Preconditions.checkArgument((value != null && failure == null) || (value == null && failure != null),
                "Only value or failure can exist at once, not both.");
        this.value = value;
        this.failure = failure;
    }

    public static <T> Result<T> success(@NotNull T value) {
        Preconditions.checkNotNull(value);
        return new Result<>(value, null);
    }

    public static <T> Result<T> failure(@NotNull Failure failure) {
        Preconditions.checkNotNull(failure);
        return new Result<>(null, failure);
    }

    // --

    public boolean isSuccess() {
        return value != null;
    }

    public boolean isFailure() {
        return failure != null;
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    public T getValueOrElse(T orElse) {
        return value != null ? value : orElse;
    }

    public T getValueOrElseGet(Supplier<T> orElseSupplier) {
        return value != null ? value : orElseSupplier.get();
    }

    public Optional<Failure> getFailure() {
        return Optional.ofNullable(failure);
    }

    public <R> R map(Function<T, R> ifValue, Function<Failure, R> ifFailure) {
        return isSuccess() ? ifValue.apply(value) : ifFailure.apply(failure);
    }

    public <R> Result<R> mapValue(Function<T, R> valueMapper) {
        //noinspection DataFlowIssue
        return isSuccess() ? success(valueMapper.apply(value)) : failure(failure);
    }

    public Result<T> mapFailure(Function<Failure, Failure> failureMapper) {
        return isFailure() ? failure(failureMapper.apply(failure)) : this;
    }

    public T handleFailure(Consumer<Failure> failureConsumer, T value) {
        if (isFailure()) {
            failureConsumer.accept(failure);
        }
        return getValueOrElse(value);
    }
}
