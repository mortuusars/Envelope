package io.github.mortuusars.envelope.util;

import net.minecraft.SharedConstants;

public abstract class Ticks {
    public static long fromSeconds(double seconds) {
        return (long) (seconds * SharedConstants.TICKS_PER_SECOND);
    }

    public static long fromMinutes(double minutes) {
        return (long) (minutes * SharedConstants.TICKS_PER_MINUTE);
    }
}
