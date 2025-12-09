package io.github.mortuusars.envelope.util.bugger.test;

import com.mojang.serialization.DataResult;

import java.util.function.Supplier;

public record Test(String name, Supplier<DataResult<Boolean>> function) {
    public static Supplier<DataResult<Boolean>> isTrue(Supplier<Boolean> function) {
        return () -> function.get() ? DataResult.success(true) : DataResult.error(() -> "Expected 'true', was 'false'.");
    }

    public static Supplier<DataResult<Boolean>> isFalse(Supplier<Boolean> function) {
        return () -> !function.get() ? DataResult.success(true) : DataResult.error(() -> "Expected 'false', was 'true'.");
    }
}
