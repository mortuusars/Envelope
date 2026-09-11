package io.github.mortuusars.envelope.util;

import net.minecraft.SharedConstants;

public abstract class Ticks {
    public static final int PER_SECOND = SharedConstants.TICKS_PER_SECOND;
    public static final int PER_MINUTE = SharedConstants.TICKS_PER_MINUTE;
    public static final int PER_IN_GAME_DAY = SharedConstants.TICKS_PER_GAME_DAY;

    public static long fromSeconds(double seconds) {
        return (long) (seconds * PER_SECOND);
    }

    public static long fromMinutes(double minutes) {
        return (long) (minutes * PER_MINUTE);
    }
}
