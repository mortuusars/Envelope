package io.github.mortuusars.envelope.util.bugger;

import java.util.function.Supplier;

public class Bugger {
    private static Supplier<Boolean> enabledSupplier = () -> false;

    public static void setup(Supplier<Boolean> isEnabled) {
        enabledSupplier = isEnabled;
    }

    // --

    public static boolean isEnabled() {
        return enabledSupplier.get();
    }
}
