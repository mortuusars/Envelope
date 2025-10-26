package io.github.mortuusars.envelope.util.bugger;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Utility to make in-game debugging easier.
 */
public class Bugger {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Supplier<Boolean> enabler = () -> false;
    public static boolean isEnabled() {
        return enabler.get();
    }
}
