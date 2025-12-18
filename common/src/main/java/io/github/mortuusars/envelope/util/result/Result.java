package io.github.mortuusars.envelope.util.result;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class Result<T> {
    @Nullable
    protected final T value;
    @Nullable
    protected final Error error;

    protected Result(@Nullable T value, @Nullable Error error) {
        Preconditions.checkArgument((value != null && error == null) || (value == null && error != null),
                "Only value or error can exist at once, not both.");
        this.value = value;
        this.error = error;
    }

    public static <T> Result<T> success(@NotNull T value) {
        Preconditions.checkNotNull(value);
        return new Result<>(value, null);
    }

    public static <T> Result<T> error(@NotNull Error error) {
        Preconditions.checkNotNull(error);
        return new Result<>(null, error);
    }

    public static <T> Result<T> error(@NotNull String errorMessage, @Nullable String errorTranslationKey) {
        Preconditions.checkNotNull(errorMessage);
        return new Result<>(null, new Error(errorMessage, errorTranslationKey));
    }

    public static <T> Result<T> error(@NotNull String errorMessage) {
        return error(errorMessage, null);
    }

    // --

    public boolean isSuccess() {
        return value != null;
    }

    public boolean isError() {
        return error != null;
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

    public Optional<Error> getError() {
        return Optional.ofNullable(error);
    }

    public Result<T> ifPresent(Consumer<T> consumer) {
        if (isSuccess()) {
            consumer.accept(value);
        }
        return this;
    }

    public Result<T> ifPresentOrElse(Consumer<T> consumer, Consumer<Error> errorConsumer) {
        if (isSuccess()) {
            consumer.accept(value);
        } else {
            errorConsumer.accept(error);
        }
        return this;
    }

    public Result<T> ifError(Consumer<Error> consumer) {
        if (isError()) {
            consumer.accept(error);
        }
        return this;
    }

    public Result<T> filter(Function<T, Result<T>> filter) {
        return getValue().map(filter).orElse(this);
    }

    public <R> R map(Function<T, R> ifValue, Function<Error, R> ifError) {
        return isSuccess() ? ifValue.apply(value) : ifError.apply(error);
    }

    public <R> Result<R> mapValue(Function<T, R> valueMapper) {
        //noinspection DataFlowIssue
        return isSuccess() ? success(valueMapper.apply(value)) : error(error);
    }

    public Result<T> mapError(Function<Error, Error> errorMapper) {
        return isError() ? error(errorMapper.apply(error)) : this;
    }

    public T handleError(Consumer<Error> errorConsumer, T value) {
        if (isError()) {
            errorConsumer.accept(error);
        }
        return getValueOrElse(value);
    }

    public DataResult<T> asDataResult() {
        return map(DataResult::success, error -> DataResult.error(error::getMessage));
    }
}
